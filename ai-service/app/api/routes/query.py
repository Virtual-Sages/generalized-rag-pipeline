from fastapi import APIRouter

from app.models.query import QueryRequest, QueryResponse
from app.services.query_service import QueryService

router = APIRouter()
query_service = QueryService()


@router.post("/query", response_model=QueryResponse)
def query(request: QueryRequest) -> QueryResponse:
    return query_service.process(request)
