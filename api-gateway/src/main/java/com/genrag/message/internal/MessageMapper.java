package com.genrag.message.internal;

import com.genrag.message.api.MessageDto;

public final class MessageMapper {
    private MessageMapper() {
    }

    public static MessageDto toDto(MessageEntity message) {
        return new MessageDto(
                message.getId(),
                message.getChat().getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
