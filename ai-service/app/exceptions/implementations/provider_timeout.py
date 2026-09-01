"""Timeout on call to LLM vendors"""

from app.exceptions.interface.provider_error import ProviderError

class ProviderTimeout(ProviderError):
    retryable = True
