"""Bad request from LLM vendors side"""

from app.exceptions.interface.provider_error import ProviderError

class ProviderBadRequest(ProviderError):
    retryable = False   # do not retry this same request
