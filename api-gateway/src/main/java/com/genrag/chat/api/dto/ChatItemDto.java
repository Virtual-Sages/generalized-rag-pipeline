package com.genrag.chat.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatItemDto(
    UUID id,
    String title,
    Instant createdAt,
    Instant updatedAt
){}