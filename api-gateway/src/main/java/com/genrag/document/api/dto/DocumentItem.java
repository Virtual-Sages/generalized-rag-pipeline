package com.genrag.document.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.genrag.document.api.DocumentStatus;

public record DocumentItem(
    UUID id,
    String fileName,
    String contentType,
    Long sizeBytes,
    DocumentStatus status,
    Instant createdAt
) {}