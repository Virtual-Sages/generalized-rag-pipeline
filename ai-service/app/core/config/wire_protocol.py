"""The request format of a vendor"""

from enum import StrEnum


class WireProtocol(StrEnum):
    """
    This will basically tell us possible protocol/setup structure 

    OPENAI_COMPAT => groq, openrouter, xAI, deepseek etc
    ANTHROPIC => anthropic
    BEDROCK => bedrock
    """

    OPENAI_COMPAT = "openai_compat"
    ANTHROPIC = "anthropic"
    BEDROCK = "bedrock"
