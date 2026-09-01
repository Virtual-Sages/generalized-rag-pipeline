"""For cases where we were not ablt to authenticate or were denied access"""

from app.exceptions.interface.provider_error import ProviderError

class ProviderAuthError(ProviderError):
    retryable = False
