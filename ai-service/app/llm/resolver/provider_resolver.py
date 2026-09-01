"""Builds all providers at start and maps them to a profile"""

from app.core.config.settings import Settings
from app.llm.factory import build_provider
from app.llm.interface.llm_provider import LLMProvider
from app.llm.resolver.resolved_provider import ResolvedProvider
from app.llm.retry.retry_policy import RetryPolicy
from app.llm.retry.retrying_provider import RetryingProvider
from app.utils.logger import get_logger

logger = get_logger(__name__)

class ProviderResolver:
    """builDS vendors and resolves them based on the profiles"""

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._instances: dict[str, LLMProvider] = {}

    def build_all(self) -> None:
        """
        Build and wrap every configured vendor
        """

        policy = RetryPolicy(
            total_budget_ms=self._settings.max_retry_budget_seconds * 1000
        )

        for name, config in self._settings.vendors.items():
            inner = build_provider(name, config)
            self._instances[name] = RetryingProvider(inner, policy)
            logger.info(
                "provider ready name=%s protocol=%s", name, config.wire_protocol.value
            )

    def resolve(self, profile_name: str | None) -> ResolvedProvider:
        profile = self._settings.profile(profile_name)
        provider = self._instances[profile.vendor]
        return ResolvedProvider(provider=provider, profile=profile)

    async def aclose(self) -> None:
        """Close every provider's HTTP pool on server shutdown"""
        for name, provider in self._instances.items():
            try:
                await provider.aclose()
            except Exception:
                logger.warning("provider close failed name=%s", name, exc_info=True)
