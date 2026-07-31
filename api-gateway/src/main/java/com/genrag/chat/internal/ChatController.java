package com.genrag.chat.internal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

import com.genrag.chat.api.ChatService;
import com.genrag.chat.api.dto.ChatItemDto;
import com.genrag.chat.api.dto.ChatMessagesDto;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chats")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public ResponseEntity<List<ChatItemDto>> getChats(
        @AuthenticationPrincipal(errorOnInvalidType = true) UUID userId) {
        try{
            return ResponseEntity.ok(chatService.getChats(userId));
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChatMessagesDto> getChatMessages(@PathVariable String id) {
       return ResponseEntity.ok(null);
    }
}