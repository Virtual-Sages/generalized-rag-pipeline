package com.genrag.document.internal;

import java.util.UUID;

/**
 * Request body sent to the AI Service's {@code POST /process} endpoint.
 * Mirrors {@code ProcessRequest} in ai-service/app/models/document.py.
 *
 * <p>{@code storagePath} is the file's name within the storage root — the
 * document id plus its extension, e.g. {@code "a1b2c3d4-….pdf"}. It is
 * deliberately <em>not</em> a path: the Orchestrator and the AI Service each
 * resolve it against their own configured root, which must point at the same
 * directory. This keeps machine-specific paths off the wire and is the same
 * shape an S3 object key would take.
 *
 * @param documentId the document to process
 * @param storagePath the file's name within the storage root
 */
record AiProcessRequest(UUID documentId, String storagePath) {}
