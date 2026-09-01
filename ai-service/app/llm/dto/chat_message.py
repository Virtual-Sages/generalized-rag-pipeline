"""One message in a chat conversation"""

from dataclasses import dataclass
from app.llm.base import Role

@dataclass(frozen=True, slots=True)
class ChatMessage:
    role: Role
    content: str
