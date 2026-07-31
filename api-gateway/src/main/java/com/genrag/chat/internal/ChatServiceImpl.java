package com.genrag.chat.internal;

import com.genrag.chat.api.ChatService;
import com.genrag.chat.api.dto.ChatItemDto;
import com.genrag.chat.api.dto.ChatMessagesDto;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class ChatServiceImpl implements ChatService {
    private final ChatRepository chatRepository;

    public ChatServiceImpl (ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    @Override
    public List<ChatItemDto> getChats(UUID userId) {
        return chatRepository.findByUser_IdOrderByUpdatedAtDesc(userId).stream()
                .map(ChatMapper::toDto)
                .toList();
    }

    @Override
    public ChatMessagesDto getChatMessages(String id) {
        throw new UnsupportedOperationException("TODO: Implement getChatMessages");
    }
}