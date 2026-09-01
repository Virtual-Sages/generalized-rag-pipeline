"""AI Facade the rest of the application calls for AI related functionality"""

from abc import ABC, abstractmethod

from app.facades.interface.ai_answer import AIAnswer

class AIFacade(ABC):
    @abstractmethod
    async def answer(
        self,
        *,
        user_id: str,
        query: str,
        request_id: str,
        profile: str | None = None,
    ) -> AIAnswer:
        """AppError will be thrown in case of any internal exception"""
