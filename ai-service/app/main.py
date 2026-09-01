from contextlib import asynccontextmanager
from uuid import uuid4

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.api.routes.query import router as query_router
from app.core.config import get_settings
from app.facades.implementations.dynamic_ai_facade import DynamicAIFacade
from app.llm.resolver import ProviderResolver
from app.models.allowed_http_response_codes import AllowedHttpResponseCodes
from app.models.query import QueryResponse
from app.services.query_service import QueryService
from app.utils.logger import get_logger

logger = get_logger(__name__)

@asynccontextmanager
async def lifespan(app: FastAPI):
    # startup, bad config leads to server crash as we are treating it as complile time issue
    settings = get_settings()
    resolver = ProviderResolver(settings)
    resolver.build_all()

    ai_facade = DynamicAIFacade(settings, resolver)

    app.state.settings = settings
    app.state.resolver = resolver
    app.state.ai_facade = ai_facade
    app.state.query_service = QueryService(ai_facade)

    yield

    await resolver.aclose()
    logger.info("ai-service stopped")


app = FastAPI(title="RAG AI Service", version="1.0.0", lifespan=lifespan)

# Incase the sprinboot server doesn't give us one, we will be making our own
def _request_id(request: Request) -> str:
    return request.headers.get("X-Request-ID") or str(uuid4())


def _envelope(request: Request, status: AllowedHttpResponseCodes, error: str) -> JSONResponse:
    body = QueryResponse(
        query="", answer="", status=status,
        request_id=_request_id(request), error=error,
    )
    return JSONResponse(status_code=200, content=body.model_dump(mode="json"))


@app.exception_handler(RequestValidationError)
async def on_validation_error(request: Request, exc: RequestValidationError) -> JSONResponse:
    return _envelope(request, AllowedHttpResponseCodes.UNPROCESSABLE_ENTITY, "Invalid request body")


@app.exception_handler(Exception)
async def on_unexpected_error(request: Request, exc: Exception) -> JSONResponse:
    message = str(exc)
    logger.exception(f"Unhandled error: {message}")
    return _envelope(request, AllowedHttpResponseCodes.INTERNAL_SERVER_ERROR, "Server Error")


app.include_router(query_router)
