package com.genrag.message.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MessageRequest(
        UUID chatId,    // null starts a new chat

        @NotBlank
        @Size(max = 8000)
        String content
) {}
