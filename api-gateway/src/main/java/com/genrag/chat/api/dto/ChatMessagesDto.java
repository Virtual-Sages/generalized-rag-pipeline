package com.genrag.chat.api.dto;

import java.util.List;
import com.genrag.message.api.MessageDto;

public record ChatMessagesDto(
    List<MessageDto> messages
){}