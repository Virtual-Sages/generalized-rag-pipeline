package com.genrag.message.api;

import com.genrag.message.api.dto.MessageRequest;
import com.genrag.message.api.dto.MessageResponse;
import com.genrag.message.internal.MessageEntity;

import java.util.UUID;
import java.util.List;

public interface MessageService {
    MessageResponse sendMessage(UUID userId, MessageRequest request);

    List<MessageEntity> getMessages(UUID chatId);
}
