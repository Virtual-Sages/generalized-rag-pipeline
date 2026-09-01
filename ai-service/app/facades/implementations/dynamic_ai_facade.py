"""AI Facade's implementation"""

from app.core.config.settings import Settings
from app.exceptions import (
    ProviderAuthError,
    ProviderBadRequest,
    ProviderError,
    ProviderRateLimited,
    ProviderTimeout,
    ProviderUnavailable,
)
from app.exceptions.interface.app_error import AppError
from app.facades.interface.ai_answer import AIAnswer
from app.facades.interface.ai_facade import AIFacade
from app.llm.base import Role
from app.llm.dto.chat_message import ChatMessage
from app.llm.dto.llm_request import LLMRequest
from app.llm.resolver.provider_resolver import ProviderResolver
from app.models.allowed_http_response_codes import AllowedHttpResponseCodes
from app.utils.logger import get_logger

logger = get_logger(__name__)

SYSTEM_PROMPT = (
    "You are a helpful support assistant. Answer the user's question "
    "concisely and accurately. If you do not know, say so."
)

class DynamicAIFacade(AIFacade):
    def __init__(
        self,
        settings: Settings,
        resolver: ProviderResolver,
        default_profile: str | None = None,
    ) -> None:
        self._settings = settings
        self._resolver = resolver
        self._default_profile = default_profile or settings.default_profile
        if self._default_profile not in settings.profiles:
            raise ValueError(
                f"{self._default_profile!r} is not in vendors.yaml"
            )

    async def answer(
        self,
        *,
        user_id: str,
        query: str,
        request_id: str,
        profile: str | None = None,
    ) -> AIAnswer:
        # Reject an extremely long prompt
        if len(query) > self._settings.max_prompt_chars:
            raise AppError(AllowedHttpResponseCodes.PAYLOAD_TOO_LARGE, "Prompt is too long")

        # Choose profile
        profile_name = self._choose_profile(profile)

        # Resolve provider + model.
        resolved = self._resolver.resolve(profile_name)

        # Make LLM Request
        request = LLMRequest(
            messages=self._build_messages(query),
            model=resolved.profile.model,
            temperature=resolved.profile.temperature,
            max_output_tokens=resolved.profile.max_output_tokens,
            request_id=request_id,
            user_id=user_id,
        )

        # Call the provider
        try:
            result = await resolved.provider.generate(request)
        except ProviderError as exc:
            app_error = self._to_app_error(exc)
            logger.warning(
                "query failed: request_id=%s \nuser_id=%s \nprofile=%s \nprovider=%s \nstatus=%d \nerror=%s",
                request_id, user_id, profile_name, exc.provider,
                int(app_error.status), type(exc).__name__,
            )
            raise app_error from exc

        logger.info(
            "query succeed: request_id=%s \nuser_id=%s \nprofile=%s \nprovider=%s \nmodel=%s \nsent_tokens=%d \nreceived_tokens=%d \nlatency_ms=%d \nattempts=%d \nquery_len=%d",
            request_id,
            user_id,
            profile_name,
            result.provider,
            result.model,
            result.usage.sent_token_count,
            result.usage.received_token_count,
            result.latency_ms,
            result.attempts,
            len(query),
        )

        return AIAnswer(text=result.text)

    def _choose_profile(self, requested: str | None) -> str:
        if requested is None:
            return self._default_profile
        if requested not in self._settings.profiles:
            raise AppError(
                AllowedHttpResponseCodes.UNPROCESSABLE_ENTITY,
                f"Invalid AI profile: {requested!r}",
            )
        return requested

    def _build_messages(self, query: str) -> list[ChatMessage]:
        return [ChatMessage(Role.SYSTEM, SYSTEM_PROMPT), ChatMessage(Role.USER, query)]

    def _to_app_error(self, exc: ProviderError) -> AppError:
        code = AllowedHttpResponseCodes
        if isinstance(exc, ProviderRateLimited):
            return AppError(code.TOO_MANY_REQUESTS, "Upstream provider rate limit reached")
        if isinstance(exc, ProviderTimeout):
            return AppError(code.GATEWAY_TIMEOUT, "Upstream provider timed out")
        if isinstance(exc, ProviderUnavailable):
            return AppError(code.BAD_GATEWAY, "Upstream provider unavailable")
        if isinstance(exc, ProviderAuthError):
            return AppError(code.INTERNAL_SERVER_ERROR, "AI service configuration error")
        if isinstance(exc, ProviderBadRequest):
            detail = str(exc).lower()
            if "length" in detail or "token" in detail or "context" in detail:
                return AppError(code.PAYLOAD_TOO_LARGE, "Prompt is too long for this model")
            return AppError(code.INTERNAL_SERVER_ERROR, "Internal error")
        return AppError(code.INTERNAL_SERVER_ERROR, "Internal error")
