package com.genrag.message.api.dto;

import com.genrag.message.api.MessageDto;

import java.util.UUID;

public record MessageResponse(
        UUID chatId,
        MessageDto userMessage,
        MessageDto assistantMessage
) {}
