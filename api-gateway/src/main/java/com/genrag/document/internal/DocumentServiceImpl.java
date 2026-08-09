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

    @Override
    public List<DocumentItem> getDocuments(UUID userId) {
        try {
            return documentRepository.findByUser_IdOrderByUpdatedAtDesc(userId).stream().map(DocumentMapper::toDto)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch documents", e);
        }
    }

    @Override
    public DocumentItem upload(MultipartFile file, UUID userId) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File cannot be empty");
            }

            String fileName = file.getOriginalFilename();
            String contentType = file.getContentType();
            Long sizeBytes = file.getSize();

            if (contentType == null || !storageService.isAllowedFormat(contentType)) {
                throw new IllegalArgumentException("File format is not allowed.");
            }

            if (sizeBytes > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("File size cannot exceed 5 MB");
            }

            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            DocumentEntity document = new DocumentEntity(
                    user,
                    fileName,
                    "resources/uploads/documents",
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

    @Override
    public Resource download(String id) {
        try {
            UUID documentId = UUID.fromString(id);

            DocumentEntity document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new IllegalArgumentException("Document not found"));
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