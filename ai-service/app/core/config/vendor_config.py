"""
Info related on how to connect one vendor like openrouter
Each object of this class will hold in for a single vendor
"""

from pydantic import BaseModel, ConfigDict, SecretStr
from app.core.config.wire_protocol import WireProtocol

class VendorConfig(BaseModel):

    model_config = ConfigDict(extra="forbid")   # this will make sure that an incorrect key raises a complie time error

    wire_protocol: WireProtocol
    base_url: str | None = None
    api_key: SecretStr | None = None
    timeout_seconds: int = 60
