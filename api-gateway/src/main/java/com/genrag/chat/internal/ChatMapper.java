package com.genrag.chat.internal;

import com.genrag.chat.api.dto.ChatItemDto;

public final class ChatMapper {
    private ChatMapper() {
    }

    public static ChatItemDto toDto(ChatEntity chat) {
        return new ChatItemDto(
            chat.getId(),
            chat.getTitle(),
            chat.getCreatedAt(),
            chat.getUpdatedAt()
        );
    }
}