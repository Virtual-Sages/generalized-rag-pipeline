package com.genrag.message.api.dto;

import java.util.UUID;

public record MessageRequest(
        UUID chatId,
        String content
) {}
