"""Adapter for any vendor that uses the OpenAI chat completions format"""

import time

import openai
from openai import AsyncOpenAI

from app.core.config.vendor_config import VendorConfig
from app.exceptions import (
    ProviderAuthError,
    ProviderBadRequest,
    ProviderError,
    ProviderRateLimited,
    ProviderTimeout,
    ProviderUnavailable,
)
from app.llm.dto.llm_request import LLMRequest
from app.llm.dto.llm_response import LLMResponse
from app.llm.dto.token_usage import TokenUsage
from app.llm.interface.llm_provider import LLMProvider


def _retry_after_ms(exc: Exception) -> int | None:
    """Read the vendor's Retry-After header, in milliseconds."""
    response = getattr(exc, "response", None)
    header = getattr(response, "headers", {}).get("retry-after") if response else None
    if not header:
        return None
    try:
        return int(float(header) * 1000)
    except ValueError:
        # Some servers send an HTTP date instead of seconds
        return None


class OpenAICompatProvider(LLMProvider):
    """
    One class for multiple vendors

    Here is the true functionality of this class, it actually is following 2 design pattern (not exactly but behaves like them):
    1. Strategy
    2. Adapter

    explanation for each:
    Strategy -> Each vendor will have a provider protocol strategy and this is one of those strategies. 
                Now one type of provider/vendor will only have 1 type of provider protocol 
                but as the system is flexible enough to support different vendors, we need to have this part setup as a strategy. 
                And each vendor in the vendors.yaml will have a place for these strategies.

    Adapter -> Now we are using classes like LlmRequest which does not follow the standard protocols because these are inner classes of our codebase.
               Even though inner classes allow us to change shape for better readability or logging 
               but still we can't send this to providers/vendors, 
               so this provider protocol class will allow us to change our custom structure to always match there standard. 

    """

    def __init__(self, name: str, client: AsyncOpenAI) -> None:
        self.name = name
        self._client = client

    # A factory method - Java would call this a static factory. The
    # constructor takes a ready client so tests can inject a fake one.
    @classmethod
    def build(cls, name: str, config: VendorConfig) -> LLMProvider:
        if not config.base_url:
            raise ValueError(f"provider {name!r}: base_url is required for openai_compat")
        if config.api_key is None:
            raise ValueError(f"provider {name!r}: api_key is required")

        return cls(
            name,
            AsyncOpenAI(
                base_url=config.base_url,
                api_key=config.api_key.get_secret_value(),
                timeout=config.timeout_seconds,
                max_retries=0       # SDK retries twice by default, we will handle retry so max retry is 0 for now, so sdk doesn't do that itself
            ),
        )

    async def generate(self, request: LLMRequest) -> LLMResponse:
        started = time.monotonic()
        try:
            completion = await self._client.chat.completions.create(
                model=request.model,
                messages=[
                    {"role": m.role.value, "content": m.content} for m in request.messages
                ],
                temperature=request.temperature,
                max_tokens=request.max_output_tokens,
            )
        except Exception as exc:
            raise self._translate(exc) from exc

        latency_ms = int((time.monotonic() - started) * 1000)

        # for validation of response format by vendor
        if not completion.choices:
            raise ProviderError("provider returned no choices", provider=self.name)

        choice = completion.choices[0]
        usage = completion.usage

        return LLMResponse(
            text=choice.message.content or "",
            finish_reason=choice.finish_reason or "stop",
            usage=TokenUsage(
                sent_token_count=usage.prompt_tokens if usage else 0,
                received_token_count=usage.completion_tokens if usage else 0,
                total_token_count=usage.total_tokens if usage else 0,
            ),
            provider=self.name,
            model=completion.model,
            latency_ms=latency_ms,
        )

    async def aclose(self) -> None:
        await self._client.close()

    # from specific child instances to broder parent instqances for exceptions
    def _translate(self, exc: Exception) -> ProviderError:
        message = str(exc)

        if isinstance(exc, openai.APITimeoutError):
            return ProviderTimeout(message, provider=self.name)
        if isinstance(exc, openai.RateLimitError):
            return ProviderRateLimited(
                message, provider=self.name, retry_after_ms=_retry_after_ms(exc)
            )
        if isinstance(exc, (openai.AuthenticationError, openai.PermissionDeniedError)):
            return ProviderAuthError(message, provider=self.name)
        if isinstance(exc, (openai.BadRequestError, openai.NotFoundError)):
            return ProviderBadRequest(message, provider=self.name)
        if isinstance(exc, (openai.APIConnectionError, openai.InternalServerError)):
            return ProviderUnavailable(message, provider=self.name)
        if isinstance(exc, openai.APIError):
            return ProviderUnavailable(message, provider=self.name)

        return ProviderError(message, provider=self.name)
