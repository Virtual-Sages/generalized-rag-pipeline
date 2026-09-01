"""Registry of embedding models under evaluation.

Every field here is load-bearing for the benchmark. In particular the prefix
fields: several of these models were trained with an instruction string glued
to the front of the input, using a different marker for queries than for
stored passages. Omitting the prefix does not raise -- it returns vectors of
the correct width that simply rank worse. Encoding the prefixes in the
registry, rather than at the call site, is what keeps the comparison fair.
"""

from dataclasses import dataclass, field


@dataclass(frozen=True)
class ModelSpec:
    key: str                      # short name used on the command line
    hf_id: str                    # HuggingFace repository id
    dim: int                      # output width, must match the pgvector column
    max_seq_tokens: int           # inputs longer than this are silently truncated
    params_millions: int
    licence: str
    query_prefix: str = ""        # prepended to search queries
    passage_prefix: str = ""      # prepended to stored passages
    trust_remote_code: bool = False
    notes: str = ""
    tags: tuple[str, ...] = field(default_factory=tuple)


REGISTRY: dict[str, ModelSpec] = {
    "minilm-l6": ModelSpec(
        key="minilm-l6",
        hf_id="sentence-transformers/all-MiniLM-L6-v2",
        dim=384,
        max_seq_tokens=256,
        params_millions=22,
        licence="Apache-2.0",
        notes="Speed and size baseline. No prefixes. Note the 256-token window, "
              "which is half what most of the field assumes.",
        tags=("local", "baseline"),
    ),
    "bge-small": ModelSpec(
        key="bge-small",
        hf_id="BAAI/bge-small-en-v1.5",
        dim=384,
        max_seq_tokens=512,
        params_millions=33,
        licence="MIT",
        query_prefix="Represent this sentence for searching relevant passages: ",
        notes="Instruction on the query side only; passages are encoded bare.",
        tags=("local",),
    ),
    "bge-base": ModelSpec(
        key="bge-base",
        hf_id="BAAI/bge-base-en-v1.5",
        dim=768,
        max_seq_tokens=512,
        params_millions=109,
        licence="MIT",
        query_prefix="Represent this sentence for searching relevant passages: ",
        notes="768 wide, so it matches the dimension the schema already assumes.",
        tags=("local", "schema-match"),
    ),
    "e5-base": ModelSpec(
        key="e5-base",
        hf_id="intfloat/e5-base-v2",
        dim=768,
        max_seq_tokens=512,
        params_millions=109,
        licence="MIT",
        query_prefix="query: ",
        passage_prefix="passage: ",
        notes="Requires prefixes on BOTH sides. Used as the prefix-ablation subject.",
        tags=("local", "prefix-sensitive"),
    ),
    "gte-base": ModelSpec(
        key="gte-base",
        hf_id="thenlper/gte-base",
        dim=768,
        max_seq_tokens=512,
        params_millions=109,
        licence="MIT",
        notes="Same size class as bge-base and e5-base but needs no prefixes, "
              "which removes a whole category of integration mistake.",
        tags=("local",),
    ),
    "nomic-v15": ModelSpec(
        key="nomic-v15",
        hf_id="nomic-ai/nomic-embed-text-v1.5",
        dim=768,
        max_seq_tokens=8192,
        params_millions=137,
        licence="Apache-2.0",
        query_prefix="search_query: ",
        passage_prefix="search_document: ",
        trust_remote_code=True,
        notes="Long context and Matryoshka training, so the output can be "
              "truncated to a narrower width post hoc. Needs remote code + einops.",
        tags=("local", "long-context", "matryoshka"),
    ),
    "mxbai-large": ModelSpec(
        key="mxbai-large",
        hf_id="mixedbread-ai/mxbai-embed-large-v1",
        dim=1024,
        max_seq_tokens=512,
        params_millions=335,
        licence="Apache-2.0",
        query_prefix="Represent this sentence for searching relevant passages: ",
        notes="Largest model that fits 4 GB of VRAM. Tests whether the extra "
              "width and depth actually buy anything on this corpus.",
        tags=("local", "large"),
    ),
    "me5-small": ModelSpec(
        key="me5-small",
        hf_id="intfloat/multilingual-e5-small",
        dim=384,
        max_seq_tokens=512,
        params_millions=118,
        licence="MIT",
        query_prefix="query: ",
        passage_prefix="passage: ",
        notes="Multilingual control. The corpus carries German and Spanish "
              "passages specifically so this has something to prove.",
        tags=("local", "multilingual"),
    ),
}

DEFAULT_ORDER = [
    "minilm-l6",
    "bge-small",
    "bge-base",
    "e5-base",
    "gte-base",
    "nomic-v15",
    "mxbai-large",
    "me5-small",
]


def resolve(names: list[str]) -> list[ModelSpec]:
    """Turn command-line model names into specs, preserving registry order."""
    if not names or names == ["all"]:
        return [REGISTRY[k] for k in DEFAULT_ORDER]

    unknown = [n for n in names if n not in REGISTRY]
    if unknown:
        raise SystemExit(
            f"unknown model(s): {', '.join(unknown)}\n"
            f"available: {', '.join(DEFAULT_ORDER)}"
        )
    return [REGISTRY[n] for n in names]
