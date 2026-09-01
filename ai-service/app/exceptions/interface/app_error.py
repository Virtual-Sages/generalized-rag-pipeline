"""what the springboot gets"""

from app.models.allowed_http_response_codes import AllowedHttpResponseCodes

class AppError(Exception):
    def __init__(self, status: AllowedHttpResponseCodes, message: str) -> None:
        super().__init__(message)
        self.status = status
        self.message = message

    def __repr__(self) -> str:
        return f"AppError(status={int(self.status)} | message={self.message!r})"
