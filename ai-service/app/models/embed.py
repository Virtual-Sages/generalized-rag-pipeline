from typing import Literal

from pydantic import BaseModel, Field


class EmbedRequest(BaseModel):
    """Input contract for the embedder POC.

    Text only. `input_type` selects which instruction prefix the model was
    trained to expect: several models place questions and stored passages in
    slightly different regions of the space, and using the wrong marker
    degrades retrieval without raising anything.
    """

    texts: list[str] = Field(
        ...,
        min_length=1,
        max_length=256,
        description="One or more texts to embed. Order is preserved in the response.",
    )
    input_type: Literal["passage", "query"] = Field(
        default="passage",
        description="Which prefix to apply. Use 'query' for search queries.",
    )


class EmbedItem(BaseModel):
    index: int
    vector: list[float]
    token_count: int
    truncated: bool


class EmbedResponse(BaseModel):
    model: str
    dim: int
    device: str
    input_type: str
    normalized: bool
    max_seq_tokens: int
    elapsed_ms: float
    items: list[EmbedItem]


class EmbedderInfo(BaseModel):
    """What the embedder would do, without loading it or embedding anything."""

    model: str
    configured_dim: int
    device: str
    max_seq_tokens: int
    batch_size: int
    query_prefix: str
    passage_prefix: str
    loaded: bool
    available: bool
    detail: str
