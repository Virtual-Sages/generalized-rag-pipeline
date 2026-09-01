"""Wire contract with the app server"""

from app.models.allowed_http_response_codes import AllowedHttpResponseCodes
from app.models.query import QueryRequest, QueryResponse

__all__ = ["AllowedHttpResponseCodes", "QueryRequest", "QueryResponse"]
