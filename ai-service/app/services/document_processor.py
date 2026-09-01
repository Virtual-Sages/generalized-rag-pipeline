"""
app/services/document_processor.py

Drives a document through the four pipeline stages, reporting each status
transition to the Orchestrator via status_client.report().

Stage table — adding a new stage is a one-line change here:
    (in_progress_status, completed_status, failed_status, stub_fn)

Mirrors the state machine in flow.mmd.
"""

import logging
import os
from uuid import UUID

from app.core.config import STORAGE_LOCATION
from app.services import status_client

logger = logging.getLogger(__name__)


# ── Stage stubs ────────────────────────────────────────────────────────────────
# Each stub is marked TODO

def _parse(document_id: UUID, storage_path: str) -> dict:
    """
    TODO: Implement document parsing (extract raw text from the file).
    Currently returns placeholder data.
    """
    logger.info("[STUB] Parsing document %s at %s", document_id, storage_path)
    return {"text": "placeholder extracted text"}


def _chunk(document_id: UUID, parsed: dict) -> list:
    """
    TODO: Implement text chunking (split text into overlapping windows).
    Currently returns placeholder data.
    """
    logger.info("[STUB] Chunking document %s", document_id)
    return [parsed.get("text", "")]


def _embed(document_id: UUID, chunks: list) -> list:
    """
    TODO: Implement embedding generation (call the embedding model).
    Currently returns placeholder data.
    """
    logger.info("[STUB] Embedding document %s (%d chunks)", document_id, len(chunks))
    return [[0.0] * 768 for _ in chunks]  # placeholder 768-dim vectors


def _index(document_id: UUID, embeddings: list) -> None:
    """
    TODO: Implement vector indexing (upsert into pgvector document_chunks).
    Currently a no-op.
    """
    logger.info("[STUB] Indexing document %s (%d embeddings)", document_id, len(embeddings))


# ── Stage table ────────────────────────────────────────────────────────────────
# Each entry: (in_progress, completed, failed, callable)
# The callable receives (document_id, previous_stage_output).
_STAGES = [
    ("PARSING",   "PARSED",   "PARSING_FAILED",   _parse),
    ("CHUNKING",  "CHUNKED",  "CHUNKING_FAILED",  _chunk),
    ("EMBEDDING", "EMBEDDED", "EMBEDDING_FAILED", _embed),
    ("INDEXING",  "INDEXED",  "INDEXING_FAILED",  _index),
]


# ── Public entry point ─────────────────────────────────────────────────────────

def process(document_id: UUID, storage_path: str) -> None:
    """
    Walk the document through every pipeline stage.

    Called from the background task scheduled by the /process route.
    All status transitions are reported to the Orchestrator; the
    Orchestrator writes them to the database.

    :param document_id:  UUID of the document to process
    :param storage_path: path (relative to STORAGE_LOCATION) where the
                         file was stored by the Orchestrator
    """
    # ── 1. Verify the file is accessible ──────────────────────────────────────
    full_path = os.path.join(STORAGE_LOCATION, storage_path) if not os.path.isabs(storage_path) else storage_path

    if not os.path.isfile(full_path):
        logger.error("Document %s not accessible at %s — reporting ACCESS_FAILED", document_id, full_path)
        status_client.report(document_id, "ACCESS_FAILED")
        return

    status_client.report(document_id, "READY_FOR_PROCESSING")

    # ── 2. Run each stage ─────────────────────────────────────────────────────
    stage_output = storage_path  # seed for the first stage

    for in_progress, completed, failed, fn in _STAGES:
        status_client.report(document_id, in_progress)
        try:
            stage_output = fn(document_id, stage_output)
            status_client.report(document_id, completed)
        except Exception:  # noqa: BLE001
            logger.exception(
                "Stage %s failed for document %s — reporting %s",
                in_progress, document_id, failed,
            )
            status_client.report(document_id, failed)
            return  # stop the pipeline; document stays at <STAGE>_FAILED

    # ── 3. All stages done ────────────────────────────────────────────────────
    status_client.report(document_id, "PROCESSED")
    logger.info("Document %s fully processed", document_id)
