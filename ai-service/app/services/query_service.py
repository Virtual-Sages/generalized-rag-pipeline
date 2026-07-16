from app.models.query import QueryRequest, QueryResponse
from app.utils.logger import get_logger

logger = get_logger(__name__)


class QueryService:
    def process(self, request: QueryRequest) -> QueryResponse:
        logger.info("Received query: %s", request.query)
        return QueryResponse(
            query=request.query,
            answer="This is a placeholder response. The RAG pipeline is not implemented yet.",
            sources=[],
        )
