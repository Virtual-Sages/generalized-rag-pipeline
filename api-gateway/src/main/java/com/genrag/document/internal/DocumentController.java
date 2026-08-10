package com.genrag.document.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.multipart.MultipartFile;

import com.genrag.document.api.DocumentService;
import com.genrag.document.api.dto.DocumentItem;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService documentService;   

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public ResponseEntity<List<DocumentItem>> getDocuments(
        @AuthenticationPrincipal(errorOnInvalidType = true) UUID userId
    ){
        return ResponseEntity.ok(documentService.getDocuments(userId));
    }

    @PostMapping
    public ResponseEntity<DocumentItem> upload(
        @RequestParam("file") MultipartFile file,
        @AuthenticationPrincipal(errorOnInvalidType = true) UUID userId
    ){
        return ResponseEntity.ok(documentService.upload(file, userId));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(
        @PathVariable String id
    ){
        return ResponseEntity.ok(documentService.download(id));
    }
}