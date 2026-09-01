"""
app/models/document.py

Pydantic models for the document processing pipeline.

ProcessRequest mirrors AiProcessRequest in
  api-gateway/.../document/internal/AiProcessRequest.java

StatusUpdate mirrors StatusUpdateRequest in
  api-gateway/.../document/api/dto/StatusUpdateRequest.java
"""

from pydantic import BaseModel
from uuid import UUID


class ProcessRequest(BaseModel):
    """Incoming request from the Orchestrator to begin processing a document."""

    documentId: UUID
    storagePath: str


class StatusUpdate(BaseModel):
    """Outgoing status report sent to the Orchestrator's internal endpoint."""

    documentId: UUID
    status: str
