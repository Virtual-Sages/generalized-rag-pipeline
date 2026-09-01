"""Interface for all LLM Provider Protocols"""

from abc import ABC, abstractmethod

from app.llm.dto.llm_request import LLMRequest
from app.llm.dto.llm_response import LLMResponse

class LLMProvider(ABC):

    # For sub classes, do not remove
    name: str

    @abstractmethod
    async def generate(self, request: LLMRequest) -> LLMResponse:
        """generate answer from LLM"""

    @abstractmethod
    async def aclose(self) -> None:
        """close http connection pool"""
