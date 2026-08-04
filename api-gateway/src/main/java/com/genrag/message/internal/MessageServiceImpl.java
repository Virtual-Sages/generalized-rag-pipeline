package com.genrag.message.internal;

import com.genrag.chat.internal.ChatEntity;
import com.genrag.chat.internal.ChatRepository;
import com.genrag.message.api.MessageDto;
import com.genrag.message.api.MessageRole;
import com.genrag.message.api.MessageService;
import com.genrag.message.api.dto.MessageRequest;
import com.genrag.message.api.dto.MessageResponse;
import com.genrag.user.internal.UserEntity;
import com.genrag.user.internal.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MessageServiceImpl implements MessageService {
    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);

    private static final String AI_UNAVAILABLE_REPLY = "Sorry, I couldn't process your request at this time.";
    private static final int TITLE_MAX_CHARS = 30;

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
        MessageEntity userMessage = new MessageEntity(chat, MessageRole.USER, request.content());
        userMessage.setCreatedAt(Instant.now());
        userMessage = messageRepository.save(userMessage);

        // 2. Call AI Service and get the response
        String assistantReply = queryAiService(request.content());

        // 3. Save the ASSISTANT message
        MessageEntity assistantMessage = new MessageEntity(chat, MessageRole.ASSISTANT, assistantReply);
        assistantMessage.setCreatedAt(Instant.now());
        assistantMessage = messageRepository.save(assistantMessage);

        // touch chat
        if (request.chatId() != null) {
            chat.setUpdatedAt(Instant.now());
        }

        return new MessageResponse(
                chat.getId(),
                MessageMapper.toDto(userMessage),
                MessageMapper.toDto(assistantMessage));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageDto> getMessages(UUID userId, UUID chatId) {
        requireOwnedChat(userId, chatId);

        return messageRepository.findByChat_IdOrderByCreatedAtAsc(chatId).stream()
                .map(MessageMapper::toDto)
                .toList();
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

        return requireOwnedChat(user.getId(), request.chatId());
    }

    /**
     * Loads {chatId} only if it belongs to {userId}.
     *
     * A chat owned by someone else is reported exactly like one that does not
     * exist,
     * so a caller cannot use this to discover other users' chat ids.
     */
    private ChatEntity requireOwnedChat(UUID userId, UUID chatId) {
        ChatEntity chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));

        if (!chat.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Chat not found or access denied");
        }

        return chat;
    }

    /**
     * Derives a short chat title from the user's message content.
     * Uses the first 30 characters or the full message, whichever is shorter.
     */
    private String deriveTitle(String content) {
        String trimmed = content.trim();

        if (trimmed.codePointCount(0, trimmed.length()) <= TITLE_MAX_CHARS) {
            return trimmed;
        }

        int end = trimmed.offsetByCodePoints(0, TITLE_MAX_CHARS);
        return trimmed.substring(0, end) + "...";
    }

    /**
     * Sends the user query to the AI Service and returns the response text.
     *
     * Never throws: an unreachable or misbehaving AI service yields the fallback
     * reply, which the caller stores as a normal assistant message. Callers get a
     * conversation that degrades rather than a failed request.
     */
    private String queryAiService(String query) {
        try {
            ResponseEntity<AiQueryResponse> response = restTemplate.postForEntity(
                    aiServiceUrl, new AiQueryRequest(query), AiQueryResponse.class);

            AiQueryResponse body = response.getBody();
            if (response.getStatusCode().is2xxSuccessful()
                    && body != null
                    && body.answer() != null) {
                return body.answer();
            }

            log.warn("AI service returned no answer (status {})", response.getStatusCode());
        } catch (Exception e) {
            log.error("AI service call to {} failed", aiServiceUrl, e);
        }

        return AI_UNAVAILABLE_REPLY;
    }
}
