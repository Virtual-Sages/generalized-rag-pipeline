from dataclasses import dataclass

@dataclass(frozen=True, slots=True)
class AIAnswer:
    text: str
