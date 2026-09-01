package com.genrag.document.api.dto;

import com.genrag.document.api.DocumentStatus;

import java.util.UUID;

/**
 * Request body for the internal status-update endpoint.
 *
 * <p>The AI Service posts this to {@code POST /api/internal/status-update}
 * whenever a pipeline stage completes or fails. The Orchestrator is the only
 * writer of the persisted status; this DTO is the courier between the two
 * services.
 *
 * @param documentId the document whose status is being reported
 * @param status     the new pipeline status (must be an internal status;
 *                   {@code FAILED} is user-visible only and will be rejected)
 */
public record StatusUpdateRequest(UUID documentId, DocumentStatus status) {}
