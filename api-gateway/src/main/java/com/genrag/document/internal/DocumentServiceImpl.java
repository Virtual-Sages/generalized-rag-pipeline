package com.genrag.document.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.genrag.document.api.DocumentService;
import com.genrag.document.api.DocumentStatus;
import com.genrag.document.api.dto.DocumentItem;
import com.genrag.document.internal.storage.StorageService;
import com.genrag.user.internal.UserEntity;
import com.genrag.user.internal.UserRepository;

@Service
public class DocumentServiceImpl implements DocumentService {
    private final DocumentRepository documentRepository;
    private final StorageService storageService;
    private final UserRepository userRepository;

    public DocumentServiceImpl(
            DocumentRepository documentRepository,
            StorageService storageService,
            UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.userRepository = userRepository;
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
                    DocumentStatus.UPLOADED);

            document = documentRepository.save(document);
            String extension = storageService.getExtension(fileName);

            storageService.upload(file, extension, document.getId().toString());

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
}