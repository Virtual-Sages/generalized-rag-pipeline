"""Reads vendors.yaml and build the Settings object"""

import os
import re
import yaml
from functools import lru_cache
from pathlib import Path
from typing import Any
from app.core.config.settings import Settings

DEFAULT_CONFIG_PATH = "config/vendors.yaml"

_VAR_PATTERN = re.compile(r"\$\{(\w+)\}")

_ALLOWED_TOP_LEVEL = {"vendors", "profiles"}

def _expand(value: Any, path: Path) -> Any:
    """Replace ${VAR} with the environment variable, everywhere."""
    if isinstance(value, dict):
        return {k: _expand(v, path) for k, v in value.items()}
    if isinstance(value, list):
        return [_expand(v, path) for v in value]
    if isinstance(value, str):

        def swap(match: re.Match[str]) -> str:
            name = match.group(1)
            try:
                return os.environ[name]
            except KeyError:
                raise ValueError(
                    f"{path} references ${{{name}}} but that environment "
                    f"variable is not set"
                ) from None

        return _VAR_PATTERN.sub(swap, value)
    return value


def _load_yaml(path: Path) -> dict[str, Any]:
    """Read the file and check its structure"""
    if not path.is_file():
        raise FileNotFoundError(
            f"config file not found: {path}"
        )

    raw = yaml.safe_load(path.read_text(encoding="utf-8"))

    if raw is None:
        raw = {}
    if not isinstance(raw, dict):
        raise ValueError(f"{path} must contain a mapping at the top level")

    unknown = sorted(set(raw) - _ALLOWED_TOP_LEVEL)
    if unknown:
        raise ValueError(f"{path} has unknown top-level keys: {unknown}")

    return _expand(raw, path)

# yet to test for stale builds
@lru_cache(maxsize=1)
def get_settings() -> Settings:
    """Build Settings from env variables and yaml file"""
    path = Path(os.environ.get("AI_CONFIG_PATH", DEFAULT_CONFIG_PATH))
    return Settings(**_load_yaml(path))
