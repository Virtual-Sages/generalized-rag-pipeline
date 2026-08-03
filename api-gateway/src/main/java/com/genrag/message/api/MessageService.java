package com.genrag.message.api;

import com.genrag.message.api.dto.MessageRequest;
import com.genrag.message.api.dto.MessageResponse;

import java.util.UUID;
import java.util.List;

public interface MessageService {
    MessageResponse sendMessage(UUID userId, MessageRequest request);

    List<MessageDto> getMessages(UUID userId, UUID chatId);
}
