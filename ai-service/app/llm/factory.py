"""Takes config and return provider object"""

from collections.abc import Callable

from app.core.config.vendor_config import VendorConfig
from app.core.config.wire_protocol import WireProtocol
from app.llm.implementations.openai_compat_provider import OpenAICompatProvider
from app.llm.interface.llm_provider import LLMProvider

# Takes name + config and returns a provider
ProviderBuilder = Callable[[str, VendorConfig], LLMProvider]

# Wire protocol related to a provider class
# Currently we haven't implemented ANTHROPIC and BEDROCK provider protocol classes
# We will need to map them here before we start to add them int the vendors.yaml

_BUILDERS: dict[WireProtocol, ProviderBuilder] = {
    WireProtocol.OPENAI_COMPAT: OpenAICompatProvider.build,
}


def build_provider(name: str, config: VendorConfig) -> LLMProvider:
    """
    Build the provider for one config entry
    """
    try:
        builder = _BUILDERS[config.wire_protocol]
    except KeyError:
        availableBuilder = ", ".join(sorted(p.value for p in _BUILDERS))
        raise ValueError(
            f"provider {name!r} uses wire_protocol {config.wire_protocol.value!r}, "
            f"which has no builder. implemented yet. Available builders - {availableBuilder}"
        ) from None
    return builder(name, config)
