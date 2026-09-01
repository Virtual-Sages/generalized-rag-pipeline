from pydantic import BaseModel, ConfigDict, Field

from app.models.allowed_http_response_codes import AllowedHttpResponseCodes


class QueryRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    query: str = Field(min_length=1)
    user_id: str = Field(min_length=1)
    profile: str | None = None


class QueryResponse(BaseModel):
    query: str
    answer: str
    status: AllowedHttpResponseCodes
    request_id: str
    error: str | None = None
