package com.genrag.message.internal;

import com.genrag.message.api.MessageService;
import com.genrag.message.api.dto.MessageRequest;
import com.genrag.message.api.dto.MessageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.util.UUID;

@RestController
@RequestMapping("/api/message")
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Processes a new user message, creating or updating a chat session.
     * The message is sent to the AI service, and the assistant's reply is returned.
     *
     * @param userId  The unique identifier of the authenticated user.
     * @param request The payload containing the optional chat ID and the message
     *                text.
     * @return A response entity containing the newly created user and assistant
     *         messages.
     */
    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @AuthenticationPrincipal(errorOnInvalidType = true) UUID userId,
            @Valid @RequestBody MessageRequest request) {
        return ResponseEntity.ok(messageService.sendMessage(userId, request));
    }
}
