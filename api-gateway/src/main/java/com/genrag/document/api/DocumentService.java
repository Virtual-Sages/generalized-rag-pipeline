package com.genrag.document.api;

import com.genrag.document.api.dto.DocumentItem;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DocumentService {
    /**
     * Retrieves all documents belonging to a user.
     *
     * @param userId the user's unique identifier
     * @return list of documents
     */
    List<DocumentItem> getDocuments(UUID userId);

    /**
     * Uploads a document for a user.
     *
     * @param file the document to upload
     * @param userId the user's unique identifier
     * @return uploaded document details
     */
    DocumentItem upload(MultipartFile file, UUID userId);

    /**
     * Downloads a document by its identifier.
     *
     * @param id the document identifier
     * @param userId the user identifier
     * @return the document resource
     */
    Resource download(String id, UUID userId);

    /**
     * Updates the pipeline status of a document.
     *
     * <p>Only internal statuses (those returned by
     * {@link DocumentStatus#getInternalStatuses()}) are accepted.
     * {@code FAILED} is user-visible only and will be rejected.
     *
     * <p>The write is additionally guarded by the state machine on
     * {@link DocumentStatus}: a status the document's current status cannot
     * legally transition to is silently ignored rather than rejected, so a
     * late or out-of-order report cannot corrupt the pipeline's record.
     *
     * @param documentId the document to update
     * @param status     the new internal status
     * @throws IllegalArgumentException if the document does not exist or
     *                                  if {@code status} is not an internal status
     */
    void updateStatus(UUID documentId, DocumentStatus status);
}
