# Embedder SPIKE — Summary and Decision

**Story:** Create a POC for the Embedder with a detailed analysis and benchmarks document.
**Status:** Complete. POC built, 8 models benchmarked on our own data, integration analysed.
**Out of scope:** chunking, text parsing, retrieval endpoint (separate stories).

This is the short version for the team. Full evidence in
[embedder-poc-analysis.md](./embedder-poc-analysis.md); concepts explained from a Java
background in [embedder-explained-for-java-devs.md](./embedder-explained-for-java-devs.md).

---

## 1. Decision

**Use `BAAI/bge-base-en-v1.5`, self-hosted, at 768 dimensions. No database change is needed.**

| | |
|---|---|
| Model | `BAAI/bge-base-en-v1.5` (109M parameters, MIT licence) |
| Dimension | **768** — already what `db/schema_init.sql` declares |
| Deployment | Self-hosted. No hosted API is a good fit (§4) |
| Storage | ~293 MB per 100,000 chunks |
| Speed | ~12.5 texts/sec on CPU, ~136 texts/sec on GPU |
| Quality | Correct passage in the top 5 for **96.7%** of test questions |

---

## 2. How we measured it

We built a test set of **65 passages and 60 questions** from content that looks like ours
(prose, PDF text, email, chat logs, tables, config docs, code, some German and Spanish),
each question tagged easy / medium / hard and labelled with the passage that answers it.

Then for each model: turn every passage into numbers, turn every question into numbers,
rank the passages by closeness, and check where the correct one landed. Two scores:

- **Recall@5** — how often the correct passage was somewhere in the top 5. **This is the
  number that matters to us**, because we hand the top 5 passages to the language model and
  it reads all of them; their internal order is irrelevant.
- **MRR@5** — the same test, but rewards putting the right answer *first*. Used as a
  tie-breaker and as a diagnostic.

### The eight candidates

Shortlisted from public leaderboards, then judged only on our own data. Everything here is
self-hostable and small enough to run on hardware we already have — 7B-class models were
excluded because they do not fit in 4 GB of VRAM.

| Model | HuggingFace id | Dim | Input window | Size | Licence | Needs prefixes? |
|---|---|---|---|---|---|---|
| `minilm-l6` | `sentence-transformers/all-MiniLM-L6-v2` | 384 | 256 | 22M | Apache-2.0 | No |
| `bge-small` | `BAAI/bge-small-en-v1.5` | 384 | 512 | 33M | MIT | Questions only |
| **`bge-base`** | `BAAI/bge-base-en-v1.5` | **768** | 512 | 109M | MIT | Questions only |
| `e5-base` | `intfloat/e5-base-v2` | 768 | 512 | 109M | MIT | Both sides |
| `gte-base` | `thenlper/gte-base` | 768 | 512 | 109M | MIT | No |
| `nomic-v15` | `nomic-ai/nomic-embed-text-v1.5` | 768 | 8192 | 137M | Apache-2.0 | Both sides |
| `mxbai-large` | `mixedbread-ai/mxbai-embed-large-v1` | 1024 | 512 | 335M | Apache-2.0 | Questions only |
| `me5-small` | `intfloat/multilingual-e5-small` | 384 | 512 | 118M | MIT | Both sides |

Three columns carry decisions rather than trivia:

- **Licence** — every candidate is MIT or Apache-2.0, so all are usable commercially. This
  was a shortlist filter: several popular embedding models carry non-commercial clauses.
- **Input window** — the number of tokens the model reads before **silently discarding the
  rest**. 512 tokens is roughly 350–400 English words. The chunking story must size chunks
  against this; note `minilm-l6`'s window is half everything else's.
- **Needs prefixes** — some models were trained with a fixed instruction string in front of
  the text, using a different one for questions than for stored passages. Getting it wrong
  raises no error and just retrieves worse. "No" is one less way to break the integration.

### Results

| Model | Dim | Recall@5 | MRR@5 | MRR on hard questions |
|---|---|---|---|---|
| **`bge-base`** ← recommended | 768 | **0.967** | 0.813 | **0.758** |
| `gte-base` | 768 | 0.933 | 0.825 | 0.714 |
| `nomic-v15` | 768 | 0.967 | 0.788 | 0.716 |
| `mxbai-large` | 1024 | 0.933 | 0.787 | 0.651 |
| `minilm-l6` | 384 | 0.933 | 0.777 | 0.740 |
| `bge-small` | 384 | 0.933 | 0.762 | 0.710 |
| `e5-base` | 768 | 0.900 | 0.748 | 0.660 |
| `me5-small` (multilingual) | 384 | 0.883 | 0.732 | 0.663 |

**These models are closer than the table looks.** With only 60 questions, any gap under
about 0.05 MRR is noise. `bge-base` and `gte-base` are a statistical tie, so the choice was
made on engineering properties rather than on the leaderboard order:

- **Best Recall@5 of anything tested (0.967)** — the metric our design actually consumes.
- **Best on the hard questions** (0.758 vs 0.714). Every model answers the easy ones.
- **Ships in the standard number format.** `gte-base` and `mxbai-large` ship a compressed
  format that runs ~5× slower on CPUs, which have no hardware support for it. We handle it
  in code, but it is a permanent trap for anyone deploying without noticing.
- **Forgiving of the most likely integration mistake.** Some models need a fixed prefix
  string in front of the text and get quietly worse without it, with no error raised.
  `bge-base` loses nothing measurable; E5-family models lose up to 9%.
- **768 dimensions** keeps the existing schema. The 1024-dim `mxbai-large` costs 33% more
  storage and scored *worse* — wider does not mean better on a narrow domain.

**Runner-up worth remembering:** `minilm-l6` scores within the noise band at **6.6× the CPU
throughput** and half the storage. If we end up CPU-only and indexing speed becomes the
bottleneck, that is the switch to make. Its input window is half the size, which the
chunking story would need to respect.

---

## 3. Speed, cost and sizing

| | CPU | GPU (RTX 3050 laptop) |
|---|---|---|
| Bulk throughput (batch 32) | 12.5 texts/sec | 136 texts/sec |
| Single request (median) | 36 ms | 17 ms |

- **Answering a user's question is fast either way** — one question is one text, ~36 ms even
  on CPU, comfortably inside a chat interaction budget.
- **Indexing documents is the slow part.** A 5 MB document is hundreds of chunks, so minutes
  on CPU. This drives the "must be asynchronous" finding in §5.
- **Batch size 32 is the right default** (set as `EMBEDDING_BATCH_SIZE`). Throughput climbs
  steeply up to 8 and flattens after 32.
- **Storage is `dimensions × 4` bytes per chunk**, and it scales forever: 293 MB per 100k
  chunks, 2.86 GB per 1M, plus the original text and the index alongside.
- **Do not raise the worker count** — each worker process loads its own full copy of the
  model. Batching inside one process is the throughput lever.

---

## 4. Why self-hosted rather than a paid API

We reviewed OpenAI, Google, Cohere and Jina. Those figures are cited from vendor
documentation, not measured — we had no API keys, and mixing cited with measured numbers is
how benchmark documents become untrustworthy. Three reasons self-hosting wins:

1. **The free tiers do not fit.** Cohere's trial key forbids production use outright;
   Google's and Jina's are fine for evaluation but become a paid dependency the day the
   product is real.
2. **Billing is per token, and it recurs.** Every query costs, and any model change forces a
   full re-encode of the entire corpus.
3. **The local models are good enough on our own data**, on hardware we already have.

---

## 5. Impact on the existing codebase

**Database: no change required.** `schema_init.sql` already declares `VECTOR(768)` and the
cosine index — the placeholder happened to be correct. Only the "PLACEHOLDER" comment needs
replacing with a note recording the decision, since changing the model later means changing
this width *and* re-embedding every row.

**Recommended addition:** store the model name alongside each vector. Without it a future
model change has no safe migration path — we cannot tell which rows are stale, so the only
option is rebuilding everything at once.

**Four findings that need follow-up work.** All were found while analysing the integration;
none are fixable inside this story:

| # | Finding | Impact |
|---|---|---|
| 1 | **Nothing triggers indexing on upload.** The gateway stores the file and returns. The `DocumentStatus` enum already defines `PROCESSING` / `INDEXED` / `FAILED`, but `UPLOADED` is the only value ever written. | Blocks the whole ingest path. Needs a trigger story. |
| 2 | **Indexing must be asynchronous.** The one existing AI call sits inside a `@Transactional` method, holding a DB connection for the length of the HTTP call. Copying that pattern for indexing would pin a connection for minutes per upload and exhaust the pool. | Design constraint on the ingestion story. |
| 3 | **The AI-service timeout properties are dead config.** `application.properties` declares connect/read timeouts overridable by env var; `RestTemplateConfig` hardcodes them and nothing binds the properties. Setting `AI_SERVICE_READ_TIMEOUT` does nothing today. | Any call slower than 60 s fails, and the documented fix is inert. Two-line change. |
| 4 | **The upload size limit is effectively hardcoded at 5 MB** in both `DocumentServiceImpl` and the frontend, below the 20 MB configurable framework limit. | Raising the env var has no effect. Small follow-up story. |

**New configuration** (added to `.env.example`): `EMBEDDING_MODEL`, `EMBEDDING_DIM`,
`EMBEDDING_BATCH_SIZE`, `EMBEDDING_DEVICE`.

---

## 6. What the next stories need from this one

- **Chunking:** size chunks in *tokens* against a 512-token window, and well under it —
  squeezing a long passage into one vector averages away the detail that makes any single
  sentence findable. Emit deterministic chunk indices so a retry is safe to repeat.
- **Parser:** plain UTF-8 text, one string per chunk. Strip repeated headers and footers or
  they land in every chunk. Preserve reading order — garbled characters survive the encoder,
  but reordered text genuinely changes the meaning.
- **Retrieval:** send questions with `input_type: "query"`, not `"passage"`. Rank with the
  cosine operator, and keep it bare in `ORDER BY` or the planner stops using the index.

---

## 7. Caveats on these numbers

- **60 questions is a small test set.** Differences under ~0.05 MRR are noise, and no part of
  this recommendation rests on a gap smaller than that.
- **The GPU numbers come from a 4 GB laptop GPU.** They establish the ordering between
  models, not production capacity. The CPU numbers are the ones that transfer.
- **Free-tier terms were verified in August 2026** and should be re-checked before we depend
  on any of them.

---

## 8. Deliverables

| Item | Location |
|---|---|
| Working embedder, `POST /embed` and `GET /embedder/info` | `app/services/embedder.py`, `app/api/routes/embed.py` |
| Benchmark harness — reproducible, tables generated rather than typed | `benchmarks/` |
| Raw measurements | `benchmarks/results/results.json` |
| Full analysis — every table, every caveat | `docs/embedder-poc-analysis.md` |
| Concepts explained for the team | `docs/embedder-explained-for-java-devs.md` |
| How the benchmarks work and how to re-run them | `docs/benchmark-methodology.md` |
| Line-by-line walkthrough of the harness code | `docs/benchmark-code-walkthrough.md` |

Re-run everything: `python run_benchmark.py --models all --device both --repeat 5`
