# Embedder benchmark harness

> **Full methodology and runbook:** [../docs/benchmark-methodology.md](../docs/benchmark-methodology.md)
> — why each measurement is taken the way it is, the fairness controls, and troubleshooting.
> This file is the quick operator reference.

Produces every number in [`../docs/embedder-poc-analysis.md`](../docs/embedder-poc-analysis.md).
If the terminology here is unfamiliar, start with
[`../docs/embedder-explained-for-java-devs.md`](../docs/embedder-explained-for-java-devs.md).
Nothing in that document is typed by hand: `run_benchmark.py` writes
`results/results.json`, and `report.py` renders it into the markdown tables
that get pasted in.

## Setup

This installs torch and the HuggingFace stack, roughly 3 GB. It goes into its
own virtual environment on purpose — `ai-service/.venv` runs the FastAPI
service and must stay light.

```powershell
cd ai-service\benchmarks
python -m venv .venv-bench
.\.venv-bench\Scripts\Activate.ps1

# GPU (recommended if you have an NVIDIA card): install torch from the CUDA
# index FIRST, otherwise pip resolves the CPU-only build from PyPI.
pip install torch --index-url https://download.pytorch.org/whl/cu128
pip install -r requirements-bench.txt
```

Confirm the GPU is actually visible before benchmarking — a silent fallback to
CPU makes the throughput numbers meaningless:

```powershell
python -c "import torch; print(torch.__version__, torch.cuda.is_available())"
```

## Running

```powershell
python run_benchmark.py --models all              # every model, CPU and GPU
python run_benchmark.py --models bge-base --device cuda
python run_benchmark.py --models e5-base --no-prefix   # prefix ablation
python report.py --ablation                       # -> results/tables.md
```

First run downloads model weights (about 2 GB total for all eight) into the
HuggingFace cache under `%USERPROFILE%\.cache\huggingface`. Later runs are
offline and fast.

## What gets measured

| Group | Metrics |
|---|---|
| Quality | Recall@1/@3/@5, MRR@5, plus MRR broken out by query difficulty |
| Speed | Single-text p50/p95 latency, throughput at batch 1/8/32/64, cold load time |
| Footprint | Peak VRAM, resident memory delta |
| Storage | Bytes per vector, projected column size at 10k/100k/1M chunks |

Quality is measured with the dot product over L2-normalised vectors, which is
arithmetically identical to the cosine similarity that pgvector computes under
`vector_cosine_ops`. The benchmark ranks the way production will rank.

## The evaluation set

`corpus/corpus.jsonl` — passages with `id`, `text`, `content_type`, `source`.
Roughly half are drawn from this repository's own documentation, schema and
code comments; the rest are domain-adjacent technical prose plus deliberately
awkward inputs (a linearised table, code, a JSON error blob, OCR-damaged text,
German and Spanish passages, a CSV fragment).

`corpus/labels.jsonl` — queries with `relevant_ids` and a `difficulty` tag.

Two properties of this set are deliberate and worth preserving if you extend it:

- **Queries avoid the passage's own vocabulary.** Questions are phrased the way
  someone would actually ask them. An eval set built by copying phrases out of
  the target passage scores every model near-perfectly and cannot distinguish
  them.
- **Near-miss distractors exist.** Several passages cover adjacent aspects of
  the same topic — three separate passages on installing the vector extension,
  three on approximate indexes. Difficulty comes from those, not from obscurity.

Chunking is held constant: one passage is one unit. That keeps chunking from
confounding the model comparison, and it is out of scope for this story.

## Extending the model list

Add a `ModelSpec` to `REGISTRY` in `models.py` and append its key to
`DEFAULT_ORDER`. Get the prefix fields right — several models were trained with
an instruction string on the query side, the passage side, or both, and
omitting it lowers quality without raising an error. `--no-prefix` exists to
demonstrate exactly how large that effect is.

The prefix table is duplicated in `../app/services/embedder.py`. Keep the two
in step when adding a model.
