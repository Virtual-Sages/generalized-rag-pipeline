from fastapi import APIRouter, HTTPException

from app.models.embed import EmbedderInfo, EmbedRequest, EmbedResponse
from app.services.embedder import (
    EmbedderUnavailableError,
    InvalidEmbedInputError,
    get_embedder,
)

router = APIRouter()


@router.post("/embed", response_model=EmbedResponse)
def embed(request: EmbedRequest) -> EmbedResponse:
    embedder = get_embedder()
    try:
        return EmbedResponse(**embedder.embed(request.texts, request.input_type))
    except InvalidEmbedInputError as exc:
        # Caller's fault and caller-fixable.
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except EmbedderUnavailableError as exc:
        # Deployment/config fault: dependencies absent, weights unreachable, or
        # a dimension mismatch. 503 rather than 500 -- it may resolve on retry
        # once the model finishes downloading.
        raise HTTPException(status_code=503, detail=str(exc)) from exc


@router.get("/embed/info", response_model=EmbedderInfo)
def embed_info() -> EmbedderInfo:
    """Report the embedder's configuration without loading the model.

    Useful for confirming which model and dimension a deployment is configured
    for before paying the load cost, and for checking whether the optional ML
    dependencies are installed at all.
    """
    embedder = get_embedder()
    config = embedder.config
    query_prefix, passage_prefix = config.prefixes

    try:
        import sentence_transformers  # noqa: F401
        available, detail = True, "sentence-transformers is installed"
    except ImportError:
        available, detail = False, (
            "sentence-transformers is not installed; POST /embed will return 503. "
            "Install with `pip install -r requirements-embed.txt`."
        )

    return EmbedderInfo(
        model=config.model_name,
        configured_dim=config.dim,
        device=embedder.device,
        max_seq_tokens=embedder.max_seq_tokens,
        batch_size=config.batch_size,
        query_prefix=query_prefix,
        passage_prefix=passage_prefix,
        loaded=embedder.is_loaded,
        available=available,
        detail=detail,
    )
