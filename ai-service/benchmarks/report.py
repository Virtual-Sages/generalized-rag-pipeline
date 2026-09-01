"""Render results/results.json into the markdown tables used by the document.

Run after run_benchmark.py. Every table in the analysis document is generated
here and pasted in; nothing is transcribed by hand, so a re-run cannot leave
the document quietly disagreeing with the data.

    python report.py                    # -> results/tables.md
    python report.py --ablation         # also diff against results-noprefix.json
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

HERE = Path(__file__).parent
RESULTS_DIR = HERE / "results"


def load(name: str) -> dict | None:
    path = RESULTS_DIR / name
    if not path.exists():
        return None
    return json.loads(path.read_text(encoding="utf-8"))


def table(headers: list[str], rows: list[list[str]]) -> str:
    line = "| " + " | ".join(headers) + " |"
    sep = "|" + "|".join("---" for _ in headers) + "|"
    body = ["| " + " | ".join(str(c) for c in r) + " |" for r in rows]
    return "\n".join([line, sep, *body])


def fmt(value, digits=3, dash="n/a"):
    if value is None:
        return dash
    if isinstance(value, float):
        return f"{value:.{digits}f}"
    return str(value)


def pick(runs: list[dict], device: str) -> list[dict]:
    return [r for r in runs if r["device"] == device]


def inventory_table(runs: list[dict]) -> str:
    seen, rows = set(), []
    for r in runs:
        if r["model"] in seen:
            continue
        seen.add(r["model"])
        prefix = "none" if not (r["query_prefix"] or r["passage_prefix"]) else (
            f"q=`{r['query_prefix'].strip() or '-'}` / "
            f"p=`{r['passage_prefix'].strip() or '-'}`"
        )
        shipped = r.get("shipped_dtype", "?").replace("torch.", "")
        rows.append([
            f"`{r['model']}`", r["hf_id"], r["dim"],
            r["max_seq_tokens_reported"] or r["max_seq_tokens_registry"],
            f"{r['params_millions']}M", shipped, r["licence"], prefix,
        ])
    return table(
        ["Key", "HuggingFace id", "Dim", "Max tokens", "Params", "Ships as",
         "Licence", "Prefixes"],
        rows,
    )


def quality_table(runs: list[dict]) -> str:
    # Quality is device-independent, so report each model once.
    seen, rows = set(), []
    for r in sorted(runs, key=lambda x: -x["quality"]["mrr@5"]):
        if r["model"] in seen:
            continue
        seen.add(r["model"])
        q = r["quality"]
        by_d = q.get("mrr_by_difficulty", {})
        rows.append([
            f"`{r['model']}`", r["dim"],
            fmt(q["recall@1"]), fmt(q["recall@3"]), fmt(q["recall@5"]),
            fmt(q["mrr@5"]),
            fmt(by_d.get("easy")), fmt(by_d.get("medium")), fmt(by_d.get("hard")),
        ])
    return table(
        ["Model", "Dim", "Recall@1", "Recall@3", "Recall@5", "MRR@5",
         "MRR easy", "MRR medium", "MRR hard"],
        rows,
    )


def speed_table(runs: list[dict], device: str) -> str:
    rows = []
    for r in sorted(pick(runs, device), key=lambda x: x["latency_single"]["p50_ms"]):
        tp = r["throughput_by_batch"]

        def rate(bs: str) -> str:
            cell = tp.get(bs, {})
            return ("OOM" if "error" in cell
                    else fmt(cell.get("texts_per_second"), 1))

        rows.append([
            f"`{r['model']}`", fmt(r["load_seconds"], 2),
            fmt(r["latency_single"]["p50_ms"], 1),
            fmt(r["latency_single"]["p95_ms"], 1),
            rate("1"), rate("8"), rate("32"), rate("64"),
        ])
    return table(
        ["Model", "Load s", "p50 ms", "p95 ms",
         "b=1 txt/s", "b=8 txt/s", "b=32 txt/s", "b=64 txt/s"],
        rows,
    )


def footprint_table(runs: list[dict]) -> str:
    rows = []
    seen = set()
    for r in runs:
        key = (r["model"], r["device"])
        if key in seen:
            continue
        seen.add(key)
        s = r["storage_projection"]
        rows.append([
            f"`{r['model']}`", r["device"], r["dim"],
            fmt(r["peak_vram_mb"], 1, dash="-"),
            fmt(r["rss_delta_mb"], 1),
            s["bytes_per_vector"],
            fmt(s["mb_per_100k_chunks"], 1),
            fmt(s["gb_per_1m_chunks"], 2),
        ])
    return table(
        ["Model", "Device", "Dim", "Peak VRAM MB", "RSS delta MB",
         "Bytes/vector", "MB @100k chunks", "GB @1M chunks"],
        rows,
    )


def environment_block(env: dict) -> str:
    keys = ["timestamp_utc", "platform", "processor", "cpu_count_logical",
            "ram_total_gb", "python", "torch", "sentence_transformers",
            "cuda_available", "gpu_name", "gpu_vram_gb", "seed"]
    rows = [[k, f"`{env[k]}`"] for k in keys if k in env]
    return table(["Field", "Value"], rows)


def ablation_table(base: dict, noprefix: dict) -> str:
    lookup = {r["model"]: r for r in noprefix["runs"]}
    rows, seen = [], set()
    for r in base["runs"]:
        other = lookup.get(r["model"])
        if not other or r["model"] in seen:
            continue
        if not (r["query_prefix"] or r["passage_prefix"]):
            continue  # nothing to ablate for prefix-free models
        seen.add(r["model"])  # quality is device-independent; report once
        with_p = r["quality"]["mrr@5"]
        without = other["quality"]["mrr@5"]
        delta = with_p - without
        rows.append([
            f"`{r['model']}`", fmt(with_p), fmt(without),
            f"{delta:+.3f}",
            f"{(delta / without * 100):+.1f}%" if without else "n/a",
        ])
    if not rows:
        return "_No prefix-using model appears in both result files._"
    return table(
        ["Model", "MRR@5 with prefixes", "MRR@5 without", "Delta", "Relative"],
        rows,
    )


def hardest_queries(runs: list[dict], limit: int = 8) -> str:
    """Queries no model placed in its top 5 -- these are eval-set bugs or genuinely hard."""
    misses: dict[str, int] = {}
    for r in runs:
        for pq in r["per_query"]:
            if pq["first_hit_rank"] is None or pq["first_hit_rank"] > 5:
                misses[pq["query"]] = misses.get(pq["query"], 0) + 1

    rows = [
        [f"_{q}_", f"{c} of {len(runs)} runs"]
        for q, c in sorted(misses.items(), key=lambda kv: -kv[1])[:limit]
    ]
    if not rows:
        return "_Every labelled query was answered within the top 5 by every model._"
    return table(["Query", "Missed in"], rows)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--ablation", action="store_true")
    args = parser.parse_args()

    base = load("results.json")
    if base is None:
        raise SystemExit("results/results.json not found -- run run_benchmark.py first")

    runs = base["runs"]
    if not runs:
        raise SystemExit("results.json contains no successful runs")

    devices = sorted({r["device"] for r in runs})
    parts = [
        "<!-- GENERATED by report.py from results/results.json. Do not edit by hand. -->",
        "",
        "### Benchmark environment", "", environment_block(base["environment"]), "",
        f"Dataset: **{base['dataset']['passages']} passages**, "
        f"**{base['dataset']['queries']} labelled queries**, content types: "
        + ", ".join(f"`{c}`" for c in base["dataset"]["content_types"]) + ".", "",
        "### Model inventory", "", inventory_table(runs), "",
        "### Retrieval quality", "", quality_table(runs), "",
    ]

    for device in devices:
        if pick(runs, device):
            parts += [f"### Speed — {device}", "", speed_table(runs, device), ""]

    parts += [
        "### Footprint and storage projection", "", footprint_table(runs), "",
        "### Queries missed by most models", "", hardest_queries(runs), "",
    ]

    if base.get("failures"):
        parts += [
            "### Failed runs", "",
            table(["Model", "Device", "Error"],
                  [[f"`{f['model']}`", f["device"], f"`{f['error']}`"]
                   for f in base["failures"]]),
            "",
        ]

    if args.ablation:
        noprefix = load("results-noprefix.json")
        if noprefix is None:
            parts += [
                "### Prefix ablation",
                "",
                "_results-noprefix.json not found; run `python run_benchmark.py --no-prefix` first._",
                "",
            ]
        else:
            parts += ["### Prefix ablation", "",
                      ablation_table(base, noprefix), ""]

    out = "\n".join(parts)
    (RESULTS_DIR / "tables.md").write_text(out, encoding="utf-8")
    print(out)
    print(f"\n[wrote {RESULTS_DIR / 'tables.md'}]")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
