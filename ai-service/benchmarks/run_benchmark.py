"""Embedder benchmark harness.

Measures retrieval quality, latency, throughput and memory footprint for each
model in the registry against the labelled corpus in corpus/, and writes a
single machine-readable results file that report.py renders into the document.

No number in the analysis document is typed by hand -- everything traces back
to results/results.json produced here.

Usage (from ai-service/benchmarks, with .venv-bench activated):

    python run_benchmark.py --models all
    python run_benchmark.py --models bge-base --device cpu --repeat 5
    python run_benchmark.py --models e5-base --no-prefix   # prefix ablation
"""

from __future__ import annotations

import argparse
import gc
import json
import platform
import statistics
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

import numpy as np
import psutil

from models import ModelSpec, resolve

HERE = Path(__file__).parent
CORPUS_PATH = HERE / "corpus" / "corpus.jsonl"
LABELS_PATH = HERE / "corpus" / "labels.jsonl"
RESULTS_DIR = HERE / "results"

SEED = 1234
WARMUP_BATCHES = 3
THROUGHPUT_BATCH_SIZES = (1, 8, 32, 64)
RECALL_KS = (1, 3, 5)
MRR_K = 5


# --------------------------------------------------------------------------
# data
# --------------------------------------------------------------------------

def load_jsonl(path: Path) -> list[dict]:
    if not path.exists():
        raise SystemExit(f"missing data file: {path}")
    rows = []
    with path.open(encoding="utf-8") as fh:
        for lineno, line in enumerate(fh, start=1):
            line = line.strip()
            if not line:
                continue
            try:
                rows.append(json.loads(line))
            except json.JSONDecodeError as exc:
                raise SystemExit(f"{path.name} line {lineno}: {exc}") from exc
    return rows


def load_dataset() -> tuple[list[dict], list[dict]]:
    corpus = load_jsonl(CORPUS_PATH)
    labels = load_jsonl(LABELS_PATH)

    ids = {row["id"] for row in corpus}
    if len(ids) != len(corpus):
        raise SystemExit("corpus.jsonl contains duplicate ids")

    # A label pointing at a passage that does not exist would silently deflate
    # every model's score, so fail loudly instead.
    for row in labels:
        missing = [rid for rid in row["relevant_ids"] if rid not in ids]
        if missing:
            raise SystemExit(
                f"labels.jsonl query {row['query']!r} references unknown "
                f"passage id(s): {', '.join(missing)}"
            )
    return corpus, labels


# --------------------------------------------------------------------------
# metrics
# --------------------------------------------------------------------------

def rank_matrix(query_vecs: np.ndarray, passage_vecs: np.ndarray) -> np.ndarray:
    """Return passage indices ordered best-first for each query.

    Vectors arrive L2-normalised, so the dot product IS the cosine similarity.
    This mirrors what pgvector computes under vector_cosine_ops, which is the
    point: the benchmark should rank the way production will rank.
    """
    sims = query_vecs @ passage_vecs.T
    return np.argsort(-sims, axis=1)


def quality_metrics(ranks: np.ndarray, labels: list[dict],
                    passage_ids: list[str]) -> dict:
    id_at = {i: pid for i, pid in enumerate(passage_ids)}
    recall_hits = {k: 0 for k in RECALL_KS}
    reciprocal_ranks = []
    per_query = []

    for qi, label in enumerate(labels):
        relevant = set(label["relevant_ids"])
        ordered = [id_at[i] for i in ranks[qi]]

        first_hit = next(
            (pos for pos, pid in enumerate(ordered, start=1) if pid in relevant),
            None,
        )

        for k in RECALL_KS:
            if first_hit is not None and first_hit <= k:
                recall_hits[k] += 1

        rr = 1.0 / first_hit if first_hit is not None and first_hit <= MRR_K else 0.0
        reciprocal_ranks.append(rr)

        per_query.append({
            "query": label["query"],
            "difficulty": label.get("difficulty", "unspecified"),
            "relevant_ids": sorted(relevant),
            "first_hit_rank": first_hit,
            "top5": ordered[:5],
        })

    n = len(labels)
    metrics = {f"recall@{k}": recall_hits[k] / n for k in RECALL_KS}
    metrics[f"mrr@{MRR_K}"] = sum(reciprocal_ranks) / n

    # Break quality out by difficulty: an aggregate that hides a collapse on the
    # hard slice is the exact failure mode the eval set was built to expose.
    by_difficulty: dict[str, list[float]] = {}
    for row, rr in zip(per_query, reciprocal_ranks):
        by_difficulty.setdefault(row["difficulty"], []).append(rr)
    metrics["mrr_by_difficulty"] = {
        d: sum(v) / len(v) for d, v in sorted(by_difficulty.items())
    }

    return {"metrics": metrics, "per_query": per_query}


# --------------------------------------------------------------------------
# timing
# --------------------------------------------------------------------------

def percentile(values: list[float], pct: float) -> float:
    if not values:
        return float("nan")
    ordered = sorted(values)
    idx = min(int(round((pct / 100.0) * (len(ordered) - 1))), len(ordered) - 1)
    return ordered[idx]


def measure_latency(encode, sample_texts: list[str], repeat: int) -> dict:
    """Single-text latency -- the query-time path, one question at a time."""
    timings = []
    for i in range(repeat * len(sample_texts)):
        text = sample_texts[i % len(sample_texts)]
        start = time.perf_counter()
        encode([text])
        timings.append((time.perf_counter() - start) * 1000.0)

    return {
        "samples": len(timings),
        "p50_ms": round(percentile(timings, 50), 3),
        "p95_ms": round(percentile(timings, 95), 3),
        "mean_ms": round(statistics.fmean(timings), 3),
        "min_ms": round(min(timings), 3),
    }


def measure_throughput(encode, texts: list[str]) -> dict:
    """Batched throughput -- the ingestion path, where batching is the lever."""
    out = {}
    for batch_size in THROUGHPUT_BATCH_SIZES:
        try:
            start = time.perf_counter()
            for i in range(0, len(texts), batch_size):
                encode(texts[i:i + batch_size])
            elapsed = time.perf_counter() - start
            out[str(batch_size)] = {
                "texts_per_second": round(len(texts) / elapsed, 1),
                "total_seconds": round(elapsed, 3),
            }
        except Exception as exc:  # OOM at large batch is a finding, not a crash
            out[str(batch_size)] = {"error": f"{type(exc).__name__}: {exc}"}
            _free_memory()
    return out


def _free_memory():
    gc.collect()
    try:
        import torch
        if torch.cuda.is_available():
            torch.cuda.empty_cache()
            torch.cuda.reset_peak_memory_stats()
    except Exception:
        pass


# --------------------------------------------------------------------------
# per-model run
# --------------------------------------------------------------------------

def run_model(spec: ModelSpec, corpus: list[dict], labels: list[dict],
              device: str, repeat: int, use_prefixes: bool) -> dict:
    import torch
    from sentence_transformers import SentenceTransformer

    print(f"\n=== {spec.key} ({spec.hf_id}) on {device} "
          f"{'' if use_prefixes else '[PREFIXES DISABLED]'}", flush=True)

    proc = psutil.Process()
    rss_before = proc.memory_info().rss
    _free_memory()

    load_start = time.perf_counter()
    model = SentenceTransformer(
        spec.hf_id,
        device=device,
        trust_remote_code=spec.trust_remote_code,
    )
    load_seconds = time.perf_counter() - load_start

    # Renamed in sentence-transformers 6.x; keep working on both.
    get_dim = getattr(model, "get_embedding_dimension", None) or \
        model.get_sentence_embedding_dimension
    reported_dim = get_dim()
    if reported_dim != spec.dim:
        print(f"  ! registry says dim={spec.dim} but model reports "
              f"{reported_dim} -- using the reported value", flush=True)

    reported_max_seq = getattr(model, "max_seq_length", None)

    # Normalise numeric precision before timing anything.
    #
    # Models do not all ship the same dtype -- thenlper/gte-base ships float16
    # while the BAAI and intfloat models ship float32. x86 CPUs have no native
    # float16 compute, so PyTorch emulates it and the model runs ~7x slower for
    # bit-identical output. Benchmarking as-shipped would compare precisions
    # rather than models. Cast everything to float32 and record what each one
    # actually shipped, so the difference is visible instead of silently
    # distorting the table.
    shipped_dtype = str(next(model[0].auto_model.parameters()).dtype)
    if shipped_dtype != "torch.float32":
        model[0].auto_model = model[0].auto_model.float()
        print(f"  cast {shipped_dtype} -> torch.float32 for a like-for-like comparison",
              flush=True)

    q_prefix = spec.query_prefix if use_prefixes else ""
    p_prefix = spec.passage_prefix if use_prefixes else ""

    def encode(texts: list[str], prefix: str = "") -> np.ndarray:
        return model.encode(
            [prefix + t for t in texts],
            batch_size=len(texts),
            normalize_embeddings=True,   # keeps dot product == cosine
            convert_to_numpy=True,
            show_progress_bar=False,
        )

    passages = [row["text"] for row in corpus]
    passage_ids = [row["id"] for row in corpus]
    queries = [row["query"] for row in labels]

    # Warm up before timing anything: the first passes trigger lazy kernel
    # initialisation and would otherwise land entirely in the p95.
    for _ in range(WARMUP_BATCHES):
        encode(passages[:8], p_prefix)

    _free_memory()
    if device == "cuda":
        torch.cuda.reset_peak_memory_stats()

    # --- quality -------------------------------------------------------
    passage_vecs = np.vstack([
        encode(passages[i:i + 32], p_prefix) for i in range(0, len(passages), 32)
    ])
    query_vecs = encode(queries, q_prefix)
    ranks = rank_matrix(query_vecs, passage_vecs)
    quality = quality_metrics(ranks, labels, passage_ids)

    # --- speed ---------------------------------------------------------
    latency = measure_latency(lambda t: encode(t, q_prefix), queries[:10], repeat)
    throughput = measure_throughput(lambda t: encode(t, p_prefix), passages)

    # --- footprint -----------------------------------------------------
    peak_vram_mb = (
        round(torch.cuda.max_memory_allocated() / 1024 / 1024, 1)
        if device == "cuda" else None
    )
    rss_delta_mb = round((proc.memory_info().rss - rss_before) / 1024 / 1024, 1)

    dim = int(passage_vecs.shape[1])
    bytes_per_vector = dim * 4
    storage = {
        "bytes_per_vector": bytes_per_vector,
        "mb_per_10k_chunks": round(bytes_per_vector * 10_000 / 1024 / 1024, 1),
        "mb_per_100k_chunks": round(bytes_per_vector * 100_000 / 1024 / 1024, 1),
        "gb_per_1m_chunks": round(bytes_per_vector * 1_000_000 / 1024**3, 2),
    }

    # Sanity check the normalisation claim rather than assuming it.
    norms = np.linalg.norm(passage_vecs, axis=1)
    norm_ok = bool(np.allclose(norms, 1.0, atol=1e-3))

    del model
    _free_memory()

    return {
        "model": spec.key,
        "hf_id": spec.hf_id,
        "device": device,
        "prefixes_applied": use_prefixes,
        "query_prefix": q_prefix,
        "passage_prefix": p_prefix,
        "dim": dim,
        "registry_dim": spec.dim,
        "shipped_dtype": shipped_dtype,
        "benchmarked_dtype": "torch.float32",
        "max_seq_tokens_reported": reported_max_seq,
        "max_seq_tokens_registry": spec.max_seq_tokens,
        "params_millions": spec.params_millions,
        "licence": spec.licence,
        "notes": spec.notes,
        "load_seconds": round(load_seconds, 2),
        "vectors_unit_norm": norm_ok,
        "quality": quality["metrics"],
        "latency_single": latency,
        "throughput_by_batch": throughput,
        "peak_vram_mb": peak_vram_mb,
        "rss_delta_mb": rss_delta_mb,
        "storage_projection": storage,
        "per_query": quality["per_query"],
    }


# --------------------------------------------------------------------------
# environment capture
# --------------------------------------------------------------------------

def environment() -> dict:
    env = {
        "timestamp_utc": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "python": sys.version.split()[0],
        "platform": platform.platform(),
        "processor": platform.processor(),
        "cpu_count_logical": psutil.cpu_count(logical=True),
        "ram_total_gb": round(psutil.virtual_memory().total / 1024**3, 1),
        "seed": SEED,
    }
    try:
        import torch
        env["torch"] = torch.__version__
        env["cuda_available"] = torch.cuda.is_available()
        if torch.cuda.is_available():
            env["gpu_name"] = torch.cuda.get_device_name(0)
            env["gpu_vram_gb"] = round(
                torch.cuda.get_device_properties(0).total_memory / 1024**3, 1
            )
    except Exception as exc:
        env["torch_error"] = str(exc)

    try:
        import sentence_transformers
        env["sentence_transformers"] = sentence_transformers.__version__
    except Exception:
        pass

    return env


# --------------------------------------------------------------------------

def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--models", nargs="+", default=["all"])
    parser.add_argument("--device", choices=["cpu", "cuda", "both"], default="both")
    parser.add_argument("--repeat", type=int, default=5,
                        help="latency repetitions per sample query")
    parser.add_argument("--no-prefix", action="store_true",
                        help="disable query/passage prefixes (ablation)")
    parser.add_argument("--output", default=None,
                        help="results filename (default results/results.json, "
                             "or results/results-noprefix.json with --no-prefix)")
    args = parser.parse_args()

    import torch
    np.random.seed(SEED)
    torch.manual_seed(SEED)

    specs = resolve(args.models)
    corpus, labels = load_dataset()
    print(f"corpus: {len(corpus)} passages | labels: {len(labels)} queries")

    devices = ["cpu", "cuda"] if args.device == "both" else [args.device]
    if "cuda" in devices and not torch.cuda.is_available():
        print("! CUDA unavailable, falling back to CPU only")
        devices = [d for d in devices if d != "cuda"] or ["cpu"]

    runs, failures = [], []
    for spec in specs:
        for device in devices:
            try:
                runs.append(run_model(spec, corpus, labels, device,
                                      args.repeat, not args.no_prefix))
                r = runs[-1]
                print(f"  recall@1={r['quality']['recall@1']:.3f} "
                      f"recall@5={r['quality']['recall@5']:.3f} "
                      f"mrr@5={r['quality']['mrr@5']:.3f} "
                      f"p50={r['latency_single']['p50_ms']}ms "
                      f"load={r['load_seconds']}s", flush=True)
            except Exception as exc:
                print(f"  FAILED: {type(exc).__name__}: {exc}", flush=True)
                failures.append({
                    "model": spec.key, "device": device,
                    "error": f"{type(exc).__name__}: {exc}",
                })
                _free_memory()

    RESULTS_DIR.mkdir(exist_ok=True)
    name = args.output or (
        "results-noprefix.json" if args.no_prefix else "results.json"
    )
    out_path = RESULTS_DIR / name
    out_path.write_text(json.dumps({
        "environment": environment(),
        "dataset": {
            "passages": len(corpus),
            "queries": len(labels),
            "content_types": sorted({r["content_type"] for r in corpus}),
        },
        "config": {
            "repeat": args.repeat,
            "warmup_batches": WARMUP_BATCHES,
            "throughput_batch_sizes": list(THROUGHPUT_BATCH_SIZES),
            "prefixes_applied": not args.no_prefix,
        },
        "runs": runs,
        "failures": failures,
    }, indent=2), encoding="utf-8")

    print(f"\nwrote {out_path}  ({len(runs)} runs, {len(failures)} failures)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
