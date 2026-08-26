package com.genrag.document.internal;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.genrag.document.api.DocumentService;
import com.genrag.document.api.DocumentStatus;
import com.genrag.document.api.dto.DocumentItem;
import com.genrag.document.internal.storage.StorageService;
import com.genrag.user.internal.UserEntity;
import com.genrag.user.internal.UserRepository;

@Service
public class DocumentServiceImpl implements DocumentService {
    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);

    private final DocumentRepository documentRepository;
    private final StorageService storageService;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final String aiServiceUrl;

    public DocumentServiceImpl(
            DocumentRepository documentRepository,
            StorageService storageService,
            UserRepository userRepository,
            RestTemplate restTemplate,
            @Value("${genrag.ai-service.url}") String aiServiceUrl) {
        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
        this.aiServiceUrl = aiServiceUrl;
    }

    /**
     * Retrieves all documents belonging to the specified user.
     *
     * @param userId the unique identifier of the user
     * @return a list of documents belonging to the user, ordered by most recently updated
     * @throws RuntimeException if the documents cannot be retrieved
     */
    @Override
    public List<DocumentItem> getDocuments(UUID userId) {
        try {
            return documentRepository.findByUser_IdOrderByUpdatedAtDesc(userId).stream().map(DocumentMapper::toDto)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch documents", e);
        }
    }

    /**
     * Uploads and stores a document for the specified user.
     *
     * The document must not be empty, must have an allowed file format,
     * and must not exceed the maximum file size limit.
     *
     * <p>After the document is saved as {@code UPLOADED}, a non-blocking
     * handoff request is sent to the AI Service at {@code POST <aiServiceUrl>/process}.
     * If the AI Service is unreachable the exception is logged and swallowed so
     * that the upload always succeeds from the caller's perspective — the document
     * stays {@code UPLOADED} and can be retried later.
     *
     * @param file the document to upload
     * @param userId the unique identifier of the user uploading the document
     * @return the uploaded document details
     * @throws IllegalArgumentException if the file is empty, the format is not allowed,
     *                                  the file exceeds the size limit, or the user is not found
     * @throws RuntimeException if the document cannot be uploaded
     */
    @Override
    public DocumentItem upload(MultipartFile file, UUID userId) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File cannot be empty");
            }

            String fileName = file.getOriginalFilename();
            String contentType = file.getContentType();
            long sizeBytes = file.getSize();

            if (contentType == null || !storageService.isAllowedFormat(contentType)) {
                throw new IllegalArgumentException("File format is not allowed.");
            }

            if (sizeBytes > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("File size cannot exceed 5 MB");
            }

            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            String filePath = storageService.getStoragePath();
            DocumentEntity document = new DocumentEntity(
                    user,
                    fileName,
                    filePath,
                    contentType,
                    sizeBytes,
                    DocumentStatus.UPLOADING);

            document = documentRepository.save(document);
            String extension = storageService.getExtension(fileName);

            try {
                storageService.upload(file, extension, document.getId().toString());
            } catch (Exception storageFailure) {
                document.setStatus(DocumentStatus.UPLOADING_FAILED);
                documentRepository.save(document);
                throw storageFailure;
            }

            document.setStatus(DocumentStatus.UPLOADED);
            document = documentRepository.save(document);

            // Hand off to the AI Service — fire-and-forget.
            // The call must never fail the upload: if the service is down the
            // document stays UPLOADED and can be reprocessed later.
            notifyAiService(document.getId(), document.getId() + extension);

            return DocumentMapper.toDto(document);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload document", e);
        }
    }

    /**
     * Retrieves a stored document by its identifier for the specified user.
     *
     * @param id the unique identifier of the document
     * @param userId the unique identifier of the user requesting the document
     * @return the document as a Spring {@link Resource}
     * @throws IllegalArgumentException if the document ID is invalid,
     *                                  the document does not exist,
     *                                  the user is not the owner of the document,
     *                                  or the stored file cannot be found
     * @throws RuntimeException if the document cannot be downloaded
     */
    @Override
    public Resource download(String id, UUID userId) {
        try {
            UserEntity user = userRepository.findById(userId).
                    orElseThrow(() -> new IllegalArgumentException("User not found"));
            UUID documentId = UUID.fromString(id);
            DocumentEntity document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new IllegalArgumentException("Document not found"));

            if (!document.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("User is not the owner of this document");
            }

            String fileName = document.getFileName();
            String extension = storageService.getExtension(fileName);

            if (!storageService.exists(id, extension)) {
                throw new IllegalArgumentException("File not found");
            }

            return storageService.download(id, extension);
        } catch (Exception e) {
            throw new RuntimeException("Download failed", e);
        }
    }

    /**
     * Updates the pipeline status of a document.
     *
     * <p>Only statuses in {@link DocumentStatus#getInternalStatuses()} are
     * accepted. {@code FAILED} is user-visible only and is rejected here,
     * mirroring the CHECK constraint on the {@code documents} table.
     *
     * <p>The write is then guarded by the state machine on
     * {@link DocumentStatus}: it lands only if the document's current status
     * may legally transition to the reported one. That blocks two things at
     * once - a late report overwriting a document that has already finished,
     * and a report arriving out of order within a run. The guard is part of
     * the UPDATE, so concurrent reports cannot both slip through.
     *
     * <p>An illegal transition is dropped, not rejected. A stale report is a
     * fire-and-forget reporter behaving correctly against a pipeline that has
     * moved on, so it is logged rather than raised. Leaving a terminal status
     * is deliberately impossible here - a reprocess must reset the document
     * through the Orchestrator, so the AI Service cannot revive a finished
     * document by reporting against it.
     *
     * @param documentId the document to update
     * @param status     the new internal status
     * @throws IllegalArgumentException if the document does not exist or
     *                                  if {@code status} is not an internal status
     */
    @Override
    @Transactional
    public void updateStatus(UUID documentId, DocumentStatus status) {
        List<DocumentStatus> internalStatuses = DocumentStatus.getInternalStatuses();
        if (!internalStatuses.contains(status)) {
            throw new IllegalArgumentException(
                    "Status '" + status + "' is not a valid internal status and cannot be persisted");
        }

        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Document not found: " + documentId));

        int updated = documentRepository.updateStatusIfAllowed(
                documentId, status, DocumentStatus.allowedPredecessorsOf(status));

        if (updated == 0) {
            DocumentStatus current = document.getStatus();
            log.warn("Document {} is {}{}; ignoring illegal transition to {}",
                    documentId, current, current.isTerminal() ? " (terminal)" : "", status);
            return;
        }

        log.info("Document {} status updated to {}", documentId, status);
    }

    /**
     * Posts a process request to the AI Service after a successful upload.
     *
     * <p>Failures are logged and swallowed so the upload always succeeds from
     * the caller's perspective. The document stays {@code UPLOADED} and can
     * be reprocessed later.
     *
     * @param documentId  the id of the document to process
     * @param storagePath the file's name within the storage root (id + extension)
     */
    private void notifyAiService(UUID documentId, String storagePath) {
        try {
            String processUrl = aiServiceUrl + "/process";
            restTemplate.postForEntity(processUrl, new AiProcessRequest(documentId, storagePath), Void.class);
            log.info("Handed document {} off to AI Service at {}", documentId, processUrl);
        } catch (Exception e) {
            log.error("AI Service handoff for document {} failed — document stays UPLOADED; reprocess later",
                    documentId, e);
        }
    }
}