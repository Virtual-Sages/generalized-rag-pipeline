"""Facade package"""

from app.facades.implementations.dynamic_ai_facade import DynamicAIFacade
from app.facades.interface.ai_answer import AIAnswer
from app.facades.interface.ai_facade import AIFacade

__all__ = ["AIFacade", "AIAnswer", "DynamicAIFacade"]
