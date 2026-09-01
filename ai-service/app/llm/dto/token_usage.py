"""For token metrics per request"""

from dataclasses import dataclass

@dataclass(frozen=True, slots=True)
class TokenUsage:
    """
    sent = system prompt + user's question + RAG
    """

    sent_token_count: int
    received_token_count: int
    total_token_count: int
