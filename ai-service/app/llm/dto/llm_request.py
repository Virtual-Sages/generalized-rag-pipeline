"""Everything a provider needs to make one call"""

from dataclasses import dataclass
from app.llm.dto.chat_message import ChatMessage

@dataclass(frozen=True, slots=True)
class LLMRequest:
    """
    Request structure for provider
    model => vendor's model id for example "llama-3.3-70b-versatile". This comes from profile for LLM model
    request_id => for logging and comparing to provider's log if they provide a view for requests.
    """

    messages: list[ChatMessage]
    model: str
    temperature: float
    max_output_tokens: int
    request_id: str
    user_id: str | None = None
