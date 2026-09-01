"""
app/services/status_client.py

Thin HTTP client that reports pipeline stage changes back to the Orchestrator.

Design rule: every call is fire-and-forget. A reporting outage must never
kill the processing pipeline; the document will simply have a stale status
in the database until the next successful report.
"""

import logging
from uuid import UUID

import httpx

from app.core.config import ORCHESTRATOR_URL

logger = logging.getLogger(__name__)

_STATUS_UPDATE_URL = f"{ORCHESTRATOR_URL}/api/internal/status-update"


def report(document_id: UUID, status: str) -> None:
    """
    POST a status update to the Orchestrator.

    Never raises: any exception is logged and swallowed so a reporting
    outage cannot kill the pipeline.

    :param document_id: UUID of the document whose status changed
    :param status:      the new DocumentStatus enum name (e.g. 'PARSING')
    """
    payload = {"documentId": str(document_id), "status": status}
    try:
        response = httpx.post(_STATUS_UPDATE_URL, json=payload, timeout=5.0)
        if not response.is_success:
            logger.warning(
                "Orchestrator returned %s for status update %s → %s",
                response.status_code,
                document_id,
                status,
            )
        else:
            logger.info("Reported %s → %s", document_id, status)
    except Exception:  # noqa: BLE001
        logger.exception(
            "Failed to report status %s for document %s — pipeline continues",
            status,
            document_id,
        )
