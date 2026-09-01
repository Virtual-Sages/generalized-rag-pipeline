"""LLM provider response"""

from dataclasses import dataclass

from app.llm.dto.token_usage import TokenUsage

@dataclass(frozen=True, slots=True)
class LLMResponse:
    """
    Answer + other metrics
    """

    text: str
    finish_reason: str
    usage: TokenUsage
    provider: str
    model: str
    latency_ms: int
    attempts: int = 1
