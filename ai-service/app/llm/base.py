"""Types shared by every LLM provider"""

from enum import StrEnum

class Role(StrEnum):
    """
    Who is author of the message
    """

    SYSTEM = "system"
    USER = "user"
    ASSISTANT = "assistant"
