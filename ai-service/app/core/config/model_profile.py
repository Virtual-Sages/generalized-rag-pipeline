"""
Profiles related info, blueprint for profile's yaml to object
One per profile key like default, heavy, fast, free etc
"""

from pydantic import BaseModel, ConfigDict

class ModelProfile(BaseModel):

    model_config = ConfigDict(extra="forbid")

    vendor: str
    model: str
    temperature: float = 0.3
    max_output_tokens: int = 2048
