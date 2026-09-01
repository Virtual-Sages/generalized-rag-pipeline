"""Embedder POC.

Turns plain text into vectors suitable for the `document_chunks.embedding`
column. Deliberately narrow: text payloads only, no chunking, no parsing, no
persistence -- those belong to their own stories.

Two design points that are not obvious:

1. `sentence_transformers` is imported lazily, inside the loader. The ML stack
   is a 2-3 GB install and is NOT in requirements.txt, so the service has to
   start and serve /query for anyone who has not installed it. A top-level
   import would make an optional POC mandatory for the whole team.

2. Vectors are L2-normalised on the way out. That makes the dot product equal
   to the cosine similarity, which is exactly what the ivfflat index in
   db/schema_init.sql computes under `vector_cosine_ops`.
"""

from __future__ import annotations

import os
import threading
import time
from dataclasses import dataclass

from app.utils.logger import get_logger

logger = get_logger(__name__)


class EmbedderUnavailableError(RuntimeError):
    """Raised when the optional ML dependencies or model weights are missing."""


class InvalidEmbedInputError(ValueError):
    """Raised for input the caller can fix -- surfaced as 4xx, not 5xx."""


# Prefixes are a property of the model, not of the caller. Keeping the table
# here means switching EMBEDDING_MODEL cannot silently drop them.
# Mirrors benchmarks/models.py -- keep the two in step.
_PREFIXES: dict[str, tuple[str, str]] = {
    "BAAI/bge-small-en-v1.5": ("Represent this sentence for searching relevant passages: ", ""),
    "BAAI/bge-base-en-v1.5": ("Represent this sentence for searching relevant passages: ", ""),
    "mixedbread-ai/mxbai-embed-large-v1": ("Represent this sentence for searching relevant passages: ", ""),
    "intfloat/e5-base-v2": ("query: ", "passage: "),
    "intfloat/multilingual-e5-small": ("query: ", "passage: "),
    "nomic-ai/nomic-embed-text-v1.5": ("search_query: ", "search_document: "),
}

MAX_BATCH = 256


@dataclass(frozen=True)
class EmbedderConfig:
    model_name: str
    dim: int
    batch_size: int
    device: str

    @classmethod
    def from_env(cls) -> "EmbedderConfig":
        return cls(
            model_name=os.getenv("EMBEDDING_MODEL", "BAAI/bge-base-en-v1.5"),
            dim=int(os.getenv("EMBEDDING_DIM", "768")),
            batch_size=int(os.getenv("EMBEDDING_BATCH_SIZE", "32")),
            device=os.getenv("EMBEDDING_DEVICE", "auto"),
        )

    @property
    def prefixes(self) -> tuple[str, str]:
        return _PREFIXES.get(self.model_name, ("", ""))


class SentenceTransformerEmbedder:
    """Wraps one sentence-transformers model as a process-wide singleton.

    Loading weights costs seconds, so the model is loaded once and reused. The
    load is guarded by a lock: without it, two concurrent first requests would
    each load a full copy and double the memory.
    """

    def __init__(self, config: EmbedderConfig | None = None) -> None:
        self._config = config or EmbedderConfig.from_env()
        self._model = None
        self._lock = threading.Lock()
        self._resolved_device = "unloaded"

    # -- lifecycle ------------------------------------------------------

    def _resolve_device(self) -> str:
        if self._config.device != "auto":
            return self._config.device
        try:
            import torch
            return "cuda" if torch.cuda.is_available() else "cpu"
        except ImportError:
            return "cpu"

    def load(self):
        """Load the model if it is not loaded yet. Safe to call repeatedly."""
        if self._model is not None:
            return self._model

        with self._lock:
            if self._model is not None:  # another thread won the race
                return self._model

            try:
                from sentence_transformers import SentenceTransformer
            except ImportError as exc:
                raise EmbedderUnavailableError(
                    "sentence-transformers is not installed. Install the "
                    "optional embedder dependencies with "
                    "`pip install -r requirements-embed.txt`."
                ) from exc

            device = self._resolve_device()
            started = time.perf_counter()
            logger.info(
                "Loading embedding model %s on %s (first run downloads weights)",
                self._config.model_name, device,
            )
            try:
                model = SentenceTransformer(self._config.model_name, device=device)
            except Exception as exc:
                raise EmbedderUnavailableError(
                    f"Could not load {self._config.model_name}: {exc}"
                ) from exc

            # Renamed in sentence-transformers 6.x; keep working on both.
            get_dim = getattr(model, "get_embedding_dimension", None) or \
                model.get_sentence_embedding_dimension
            actual_dim = get_dim()

            # Some models ship float16 weights (thenlper/gte-base does). x86
            # CPUs have no native float16 compute, so PyTorch emulates it and
            # the model runs roughly 7x slower for output that is identical to
            # within 1e-4 cosine. Cast to float32 when running on CPU; leave
            # float16 alone on GPU, where it is natively supported and faster.
            if device == "cpu":
                try:
                    param_dtype = next(model[0].auto_model.parameters()).dtype
                    if str(param_dtype) != "torch.float32":
                        model[0].auto_model = model[0].auto_model.float()
                        logger.info(
                            "Cast %s weights from %s to float32 for CPU inference "
                            "(float16 is emulated on x86 and ~7x slower)",
                            self._config.model_name, param_dtype,
                        )
                except (AttributeError, IndexError, StopIteration):
                    # Non-standard module layout; not worth failing the load over.
                    logger.debug("Could not inspect weight dtype; leaving as shipped")
            if actual_dim != self._config.dim:
                # Fail loudly here rather than at INSERT time: a dimension
                # mismatch against the VECTOR(n) column is rejected by the
                # database, and diagnosing that from a failed ingest is worse.
                raise EmbedderUnavailableError(
                    f"EMBEDDING_DIM is {self._config.dim} but "
                    f"{self._config.model_name} produces {actual_dim}. Set "
                    f"EMBEDDING_DIM={actual_dim} and make sure the "
                    f"document_chunks.embedding column matches."
                )

            self._model = model
            self._resolved_device = device
            logger.info(
                "Loaded %s (dim=%d, max_seq=%s) in %.2fs",
                self._config.model_name, actual_dim,
                getattr(model, "max_seq_length", "?"),
                time.perf_counter() - started,
            )
            return self._model

    # -- introspection --------------------------------------------------

    @property
    def config(self) -> EmbedderConfig:
        return self._config

    @property
    def is_loaded(self) -> bool:
        return self._model is not None

    @property
    def device(self) -> str:
        return self._resolved_device

    @property
    def max_seq_tokens(self) -> int:
        if self._model is None:
            return 0
        return int(getattr(self._model, "max_seq_length", 0) or 0)

    def count_tokens(self, texts: list[str]) -> list[int]:
        """Token counts BEFORE truncation, so callers can detect dropped text."""
        model = self.load()
        tokenizer = model.tokenizer
        encoded = tokenizer(texts, add_special_tokens=True, truncation=False)
        return [len(ids) for ids in encoded["input_ids"]]

    # -- the actual work ------------------------------------------------

    def embed(self, texts: list[str], input_type: str = "passage") -> dict:
        self._validate(texts)
        model = self.load()

        query_prefix, passage_prefix = self._config.prefixes
        prefix = query_prefix if input_type == "query" else passage_prefix

        # Count on the prefixed text: the prefix consumes budget too.
        prepared = [prefix + text for text in texts]
        token_counts = self.count_tokens(prepared)
        limit = self.max_seq_tokens or 512

        started = time.perf_counter()
        vectors = model.encode(
            prepared,
            batch_size=self._config.batch_size,
            normalize_embeddings=True,
            convert_to_numpy=True,
            show_progress_bar=False,
        )
        elapsed_ms = (time.perf_counter() - started) * 1000.0

        truncated = [count > limit for count in token_counts]
        if any(truncated):
            logger.warning(
                "%d of %d input(s) exceeded the %d-token window and were "
                "truncated; the discarded tail is not represented in the vector",
                sum(truncated), len(texts), limit,
            )

        return {
            "model": self._config.model_name,
            "dim": int(vectors.shape[1]),
            "device": self._resolved_device,
            "input_type": input_type,
            "normalized": True,
            "max_seq_tokens": limit,
            "elapsed_ms": round(elapsed_ms, 3),
            "items": [
                {
                    "index": i,
                    "vector": vector.tolist(),
                    "token_count": token_counts[i],
                    "truncated": truncated[i],
                }
                for i, vector in enumerate(vectors)
            ],
        }

    @staticmethod
    def _validate(texts: list[str]) -> None:
        if not texts:
            raise InvalidEmbedInputError("texts must contain at least one item")
        if len(texts) > MAX_BATCH:
            raise InvalidEmbedInputError(
                f"batch of {len(texts)} exceeds the maximum of {MAX_BATCH}"
            )
        for i, text in enumerate(texts):
            if not isinstance(text, str):
                raise InvalidEmbedInputError(f"texts[{i}] is not a string")
            if not text.strip():
                # An all-whitespace input yields a vector that is not
                # meaningfully "about" anything but still ranks against real
                # queries. Refuse it rather than poison the index.
                raise InvalidEmbedInputError(f"texts[{i}] is empty or whitespace-only")


_embedder = SentenceTransformerEmbedder()


def get_embedder() -> SentenceTransformerEmbedder:
    return _embedder
