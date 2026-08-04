package com.genrag.chat.internal;

import com.genrag.chat.api.ChatService;
import com.genrag.chat.api.dto.ChatItemDto;
import com.genrag.chat.api.dto.ChatMessagesDto;
import com.genrag.message.api.MessageDto;
import com.genrag.message.api.MessageService;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class ChatServiceImpl implements ChatService {
    private final ChatRepository chatRepository;
    private final MessageService messageService;

    public ChatServiceImpl (ChatRepository chatRepository, MessageService messageService) {
        this.chatRepository = chatRepository;
        this.messageService = messageService;
    }

    @Override
    public List<ChatItemDto> getChats(UUID userId) {
        return chatRepository.findByUser_IdOrderByUpdatedAtDesc(userId).stream()
                .map(ChatMapper::toDto)
                .toList();
    }

    @Override
    public ChatMessagesDto getChatMessages(UUID userId, String id) {
        UUID chatId = UUID.fromString(id);
        List<MessageDto> messages = messageService.getMessages(userId, chatId);

        return new ChatMessagesDto(messages);
    }
}