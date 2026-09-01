"""Base for all provider related exceptions"""


class ProviderError(Exception):

    retryable: bool = False

    def __init__(
        self,
        message: str,
        *,
        provider: str,
        retry_after_ms: int | None = None,
    ) -> None:
        super().__init__(message)
        self.message = message
        self.provider = provider
        self.retry_after_ms = retry_after_ms

    # for debugging
    def __repr__(self) -> str:
        return (
            f"{type(self).__name__}(provider={self.provider!r}, "
            f"retryable={self.retryable}, retry_after_ms={self.retry_after_ms})"
        )
