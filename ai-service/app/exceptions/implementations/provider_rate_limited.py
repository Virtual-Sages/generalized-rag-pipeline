"""LLM vendors ratelimited"""

from app.exceptions.interface.provider_error import ProviderError

class ProviderRateLimited(ProviderError):
    retryable = True
