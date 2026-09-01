from uuid import uuid4

from app.exceptions.interface.app_error import AppError
from app.facades.interface.ai_facade import AIFacade
from app.models.allowed_http_response_codes import AllowedHttpResponseCodes
from app.models.query import QueryRequest, QueryResponse
from app.utils.logger import get_logger

logger = get_logger(__name__)


class QueryService:
    def __init__(self, ai_facade: AIFacade) -> None:
        self._ai_facade = ai_facade

    async def answer(self, body: QueryRequest, request_id: str | None = None) -> QueryResponse:
        request_id = request_id or str(uuid4())

        try:
            answer = await self._ai_facade.answer(
                user_id=body.user_id,
                query=body.query,
                request_id=request_id,
                profile=body.profile,
            )
        except AppError as exc:
            # In case of exception status code + message are sent in response body and not as the direct status code.
            # Return is always code 200 based and Springboot server can retrieve code from the response body
            return QueryResponse(
                query=body.query,
                answer="",
                status=exc.status,
                request_id=request_id,
                error=exc.message,
            )
        except Exception:
            logger.exception("unhandled error request_id=%s", request_id)
            return QueryResponse(
                query=body.query,
                answer="",
                status=AllowedHttpResponseCodes.INTERNAL_SERVER_ERROR,
                request_id=request_id,
                error="Internal error",
            )

        return QueryResponse(
            query=body.query,
            answer=answer.text,
            status=AllowedHttpResponseCodes.OK,
            request_id=request_id,
            error=None,
        )
