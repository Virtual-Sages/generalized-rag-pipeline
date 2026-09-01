"""Exceptions module"""

from app.exceptions.implementations.provider_auth_error import ProviderAuthError
from app.exceptions.implementations.provider_bad_request import ProviderBadRequest
from app.exceptions.implementations.provider_rate_limited import ProviderRateLimited
from app.exceptions.implementations.provider_timeout import ProviderTimeout
from app.exceptions.implementations.provider_unavailable import ProviderUnavailable
from app.exceptions.interface.app_error import AppError
from app.exceptions.interface.provider_error import ProviderError

__all__ = [
    "AppError",
    "ProviderError",
    "ProviderRateLimited",
    "ProviderTimeout",
    "ProviderUnavailable",
    "ProviderAuthError",
    "ProviderBadRequest",
]
