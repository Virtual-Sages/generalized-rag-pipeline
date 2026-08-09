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
     * @return the document resource
     */
    Resource download(String id);
}