package com.genrag.document.api;

import com.genrag.document.api.dto.DocumentItem;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DocumentService {
    List<DocumentItem> getDocuments(UUID userId);
    DocumentItem upload(MultipartFile file, UUID userId);
    Resource download(String id);
}