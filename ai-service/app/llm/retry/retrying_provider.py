"""A provider that retries a failing provider."""

import asyncio
import random
import time
from dataclasses import replace

from app.exceptions import ProviderError
from app.llm.dto.llm_request import LLMRequest
from app.llm.dto.llm_response import LLMResponse
from app.llm.interface.llm_provider import LLMProvider
from app.llm.retry.retry_policy import RetryPolicy
from app.utils.logger import get_logger

logger = get_logger(__name__)


# Decorator: it IS a provider (implements LLMProvider) and HOLDS a provider
# (_inner). Because it satisfies the same interface it wraps, everything
# above sees a plain provider and never knows retries are happening.
class RetryingProvider(LLMProvider):
    """Wraps another provider and retries transient failures.

    Only retries when the error says it is safe to (exc.retryable). An auth
    error or a bad request fails the same way on attempt two, so those are
    re-raised at once.
    """

    def __init__(self, inner: LLMProvider, policy: RetryPolicy) -> None:
        self._inner = inner
        self._policy = policy
        # Same name as what it wraps - the wrapper is invisible.
        self.name = inner.name

    async def generate(self, request: LLMRequest) -> LLMResponse:
        # Stamp the start BEFORE the first attempt. The budget is total
        # wall-clock, so it counts the time spent waiting for responses,
        # not just the pauses between tries.
        started = time.monotonic()

        for attempt in range(1, self._policy.max_attempts + 1):
            try:
                result = await self._inner.generate(request)
                # Record which attempt finally worked, for the service log.
                return replace(result, attempts=attempt)
            except ProviderError as exc:
                # Not retryable, or out of attempts -> give up now.
                if not exc.retryable or attempt >= self._policy.max_attempts:
                    raise

                delay_s = self._delay_seconds(attempt, exc.retry_after_ms)

                # Would waiting push us past the budget? If so, stop now and
                # re-raise rather than start a wait we cannot afford.
                elapsed_ms = (time.monotonic() - started) * 1000
                if elapsed_ms + delay_s * 1000 > self._policy.total_budget_ms:
                    raise

                logger.warning(
                    "provider retry request_id=%s provider=%s attempt=%d/%d "
                    "error=%s delay_ms=%d",
                    request.request_id, self.name, attempt,
                    self._policy.max_attempts, type(exc).__name__, int(delay_s * 1000),
                )
                await asyncio.sleep(delay_s)

        # The loop always returns or raises above; this is unreachable.
        raise AssertionError("retry loop exited without returning or raising")

    async def aclose(self) -> None:
        await self._inner.aclose()

    def _delay_seconds(self, attempt: int, retry_after_ms: int | None) -> float:
        """How long to wait before the next attempt, in seconds.

        If the vendor told us how long to wait (Retry-After on a 429),
        honour that. Otherwise use exponential backoff with full jitter:
        a random point in [0, ceiling], where the ceiling doubles each
        attempt up to max_delay_ms. The randomness stops many clients that
        failed together from retrying in lockstep.
        """
        if retry_after_ms is not None:
            return retry_after_ms / 1000

        ceiling_ms = min(
            self._policy.base_delay_ms * 2 ** (attempt - 1),
            self._policy.max_delay_ms,
        )
        return random.uniform(0, ceiling_ms) / 1000
