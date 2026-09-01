"""We were not able to reach the vendor, it may be due to unavilability of client or a connection error"""

from app.exceptions.interface.provider_error import ProviderError

class ProviderUnavailable(ProviderError):
    retryable = True
