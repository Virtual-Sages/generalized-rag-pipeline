"""
app/core/config.py

Central configuration for the AI Service, read from environment variables
(or their defaults). Add new variables here; keep secrets out of source.
"""

import os

# URL of the Orchestrator (API Gateway).
# The AI Service posts status updates to:
#   POST <ORCHESTRATOR_URL>/api/internal/status-update
ORCHESTRATOR_URL: str = os.getenv("ORCHESTRATOR_URL", "http://localhost:8080")

# Root directory where uploaded files are stored.
# The Orchestrator sends a storagePath relative to this location, so this must
# resolve to the SAME directory as the Orchestrator's storage.location.
# The default is relative to ai-service/, the directory uvicorn is started from.
STORAGE_LOCATION: str = os.getenv(
    "STORAGE_LOCATION", "../api-gateway/src/main/resources/uploads/documents"
)
