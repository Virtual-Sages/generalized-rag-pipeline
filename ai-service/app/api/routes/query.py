from typing import Annotated
from uuid import uuid4

from fastapi import APIRouter, Depends, Header, Request

from app.models.query import QueryRequest, QueryResponse
from app.services.query_service import QueryService

router = APIRouter()

def get_query_service(request: Request) -> QueryService:
    return request.app.state.query_service

@router.post("/query", response_model=QueryResponse)
async def query(
    body: QueryRequest,
    query_service: Annotated[QueryService, Depends(get_query_service)],
    x_request_id: Annotated[str | None, Header(alias="X-Request-ID")] = None,   # for tracking
) -> QueryResponse:
    request_id = x_request_id or str(uuid4())
    return await query_service.answer(body, request_id)
