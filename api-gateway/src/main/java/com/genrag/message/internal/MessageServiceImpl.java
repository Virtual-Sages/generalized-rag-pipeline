package com.genrag.message.internal;

import com.genrag.chat.internal.ChatEntity;
import com.genrag.chat.internal.ChatRepository;
import com.genrag.message.api.MessageRole;
import com.genrag.message.api.MessageService;
import com.genrag.message.api.dto.MessageRequest;
import com.genrag.message.api.dto.MessageResponse;
import com.genrag.user.internal.UserEntity;
import com.genrag.user.internal.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MessageServiceImpl implements MessageService {
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final String aiServiceUrl;

    public MessageServiceImpl(
            MessageRepository messageRepository,
            ChatRepository chatRepository,
            UserRepository userRepository,
            RestTemplate restTemplate,
            @Value("${genrag.ai-service.url}") String aiServiceUrl) {
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
        this.aiServiceUrl = aiServiceUrl;
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(UUID userId, MessageRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatEntity chat = resolveChat(user, request);

        // 1. Save the USER message
        MessageEntity userMessage = messageRepository.save(
                new MessageEntity(chat, MessageRole.USER, request.content()));

        // 2. Call AI Service and get the response
        String assistantReply = queryAiService(request.content());

        // 3. Save the ASSISTANT message
        MessageEntity assistantMessage = messageRepository.save(
                new MessageEntity(chat, MessageRole.ASSISTANT, assistantReply));

        return new MessageResponse(
                chat.getId(),
                MessageMapper.toDto(userMessage),
                MessageMapper.toDto(assistantMessage));
    }

    /**
     * Resolves or creates the chat for the given message request.
     *
     * If chatId is null, a new chat is created with a title derived from the
     * first few words of the user's message. Otherwise, the existing chat is
     * fetched and ownership is validated against the authenticated user.
     */
    private ChatEntity resolveChat(UserEntity user, MessageRequest request) {
        if (request.chatId() == null) {
            String title = deriveTitle(request.content());
            ChatEntity chat = new ChatEntity(user, title);
            return chatRepository.save(chat);
        }

        ChatEntity chat = chatRepository.findById(request.chatId())
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));

        if (!chat.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Chat not found or access denied");
        }

        return chat;
    }

    @Override
    public List<MessageEntity> getMessages(UUID chatId) {
        return messageRepository.findByChat_IdOrderByCreatedAtAsc(chatId);
    }

    /**
     * Derives a short chat title from the user's message content.
     * Uses the first 30 characters or the full message, whichever is shorter.
     */
    private String deriveTitle(String content) {
        return content.length() > 30
                ? content.substring(0, 30) + "..."
                : content;
    }

    /**
     * Sends the user query to the AI Service and returns the response text.
     * Falls back to a default error message if the service is unavailable.
     */
    private String queryAiService(String query) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> httpRequest = new HttpEntity<>(
                Map.of("query", query), headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    aiServiceUrl,
                    org.springframework.http.HttpMethod.POST,
                    httpRequest,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object answer = response.getBody().get("answer");
                if (answer != null) {
                    return answer.toString();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("AI service is unavailable", e);
        }

        return "Sorry, I couldn't process your request at this time.";
    }
}
