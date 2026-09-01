from dataclasses import dataclass

@dataclass(frozen=True, slots=True)
class RetryPolicy:
    total_budget_ms: int
    max_attempts: int = 3
    base_delay_ms: int = 250
    max_delay_ms: int = 4_000
