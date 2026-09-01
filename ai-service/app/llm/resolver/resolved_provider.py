from dataclasses import dataclass

from app.core.config.model_profile import ModelProfile
from app.llm.interface.llm_provider import LLMProvider

@dataclass(frozen=True, slots=True)
class ResolvedProvider:
    provider: LLMProvider
    profile: ModelProfile
