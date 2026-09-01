"""Main object that is contains all the required info for the ai-service to run"""

from pydantic import SecretStr, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

from app.core.config.model_profile import ModelProfile
from app.core.config.vendor_config import VendorConfig
from app.core.config.wire_protocol import WireProtocol

class Settings(BaseSettings):
    """
    Environment variables for the service + the two maps loaded from YAML
    """

    model_config = SettingsConfigDict(
        env_prefix="AI_", env_file=".env", extra="ignore"
    )

    service_name: str = "ai-service"
    env: str = "development"
    internal_api_key: SecretStr
    max_prompt_chars: int = 8_000
    request_timeout_seconds: int = 60
    max_retry_budget_seconds: int = 60
    default_profile: str = "default"
    vendors: dict[str, VendorConfig] = {}
    profiles: dict[str, ModelProfile] = {}

    def profile(self, name: str | None) -> ModelProfile:
        """Look up a profile by name, None means default profile"""
        key = name or self.default_profile
        try:
            return self.profiles[key]
        except KeyError:
            raise KeyError(f"Unknown profile {key!r}") from None

    # Wrong YAML -> inform at startup -> dont start the application because config is wrong
    @model_validator(mode="after")
    def _check_config_is_consistent(self) -> "Settings":
        # check vendor for each profile
        for name, profile in self.profiles.items():
            if profile.vendor not in self.vendors:
                raise ValueError(
                    f"Profile {name!r} is configured with an unsupported vendor: {profile.vendor!r}."
                )

        # check if default profile is one of the supported profiles
        if self.profiles and self.default_profile not in self.profiles:
            raise ValueError(
                f"Default profile {self.default_profile!r} is not a recognized profile."
            )

        # check that for vendors using OpenAICompact format, they must have base_url
        for name, vendor in self.vendors.items():
            if vendor.wire_protocol is WireProtocol.OPENAI_COMPAT and not vendor.base_url:
                raise ValueError(
                    f"vendor {name!r} uses openai_compat, which needs a base_url"
                )

        # basic value checks 
        if self.request_timeout_seconds <= 0:
            raise ValueError("request_timeout_seconds must be positive")

        if self.max_retry_budget_seconds <= 0:
            raise ValueError("max_retry_budget_seconds must be positive")

        if self.max_retry_budget_seconds >= self.request_timeout_seconds:
            raise ValueError(
                f"max_retry_budget_seconds={self.max_retry_budget_seconds} must be below request_timeout_seconds={self.request_timeout_seconds}"
            )

        for name, vendor in self.vendors.items():
            if vendor.timeout_seconds >= self.request_timeout_seconds:
                raise ValueError(
                    f"Vendor {name!r} timeout_seconds={vendor.timeout_seconds} must be below request_timeout_seconds={self.request_timeout_seconds}"
                )

        return self
