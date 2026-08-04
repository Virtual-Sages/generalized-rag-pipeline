package com.genrag.chat.api;

import com.genrag.chat.api.dto.ChatItemDto;
import com.genrag.chat.api.dto.ChatMessagesDto;

import java.util.List;
import java.util.UUID;

public interface ChatService {
    List<ChatItemDto> getChats(UUID userId);
    ChatMessagesDto getChatMessages(UUID userId, String chatId);
}