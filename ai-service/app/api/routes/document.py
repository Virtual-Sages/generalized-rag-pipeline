"""
app/api/routes/document.py

POST /process — receives a document handoff from the Orchestrator and
schedules the stage-walker as a FastAPI BackgroundTask.

Returns 202 Accepted immediately so the Orchestrator never blocks on
the full pipeline duration.
"""

from fastapi import APIRouter, BackgroundTasks
from fastapi.responses import Response

from app.models.document import ProcessRequest
from app.services import document_processor

router = APIRouter()


@router.post("/process", status_code=202)
def process_document(
    request: ProcessRequest,
    background_tasks: BackgroundTasks,
) -> Response:
    """
    Accepts a document processing request from the Orchestrator.

    Schedules ``document_processor.process`` as a background task and
    returns ``202 Accepted`` immediately — the Orchestrator is not blocked
    while the pipeline runs.

    The pipeline reports each stage transition back to the Orchestrator via
    ``POST /api/internal/status-update``; the Orchestrator writes status to
    the database.
    """
    background_tasks.add_task(
        document_processor.process,
        request.documentId,
        request.storagePath,
    )
    return Response(status_code=202)
