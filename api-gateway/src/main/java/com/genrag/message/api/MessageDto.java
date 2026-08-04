package com.genrag.message.api;

import java.time.Instant;
import java.util.UUID;

public record MessageDto(
        UUID id,
        UUID chatId,
        MessageRole role,
        String content,
        Instant createdAt
) {}
