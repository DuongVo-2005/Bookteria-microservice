package com.devteria.chat.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.corundumstudio.socketio.SocketIOServer;
import com.devteria.chat.dto.ConversationStatus;
import com.devteria.chat.dto.request.ChatMessageRequest;
import com.devteria.chat.dto.response.ChatMessageResponse;
import com.devteria.chat.entity.ChatMessage;
import com.devteria.chat.entity.Conversation;
import com.devteria.chat.entity.ParticipantInfo;
import com.devteria.chat.entity.WebSocketSession;
import com.devteria.chat.exception.AppException;
import com.devteria.chat.exception.ErrorCode;
import com.devteria.chat.mapper.ChatMessageMapper;
import com.devteria.chat.repository.ChatMessageRepository;
import com.devteria.chat.repository.ConversationRepository;
import com.devteria.chat.repository.WebSocketSessionRepository;
import com.devteria.chat.repository.httpclient.FriendClient;
import com.devteria.chat.repository.httpclient.ProfileClient;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatMessageService {

    ChatMessageRepository chatMessageRepository;

    ConversationRepository conversationRepository;

    ChatMessageMapper chatMessageMapper;

    ProfileClient profileClient;

    FriendClient friendClient;

    SocketIOServer socketIOServer;

    WebSocketSessionRepository webSocketSessionRepository;

    ObjectMapper objectMapper;

    public List<ChatMessageResponse> getMessages(String conversationId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND))
                .getParticipants()
                .stream()
                .filter(participantInfo -> userId.equals(participantInfo.getUserId()))
                .findAny()
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        var messages = chatMessageRepository.findAllByConversationIdOrderByCreatedDateDesc(conversationId);

        return messages.stream().map(this::toChatMessageResponse).toList();
    }

    public ChatMessageResponse create(ChatMessageRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        // validate conversationId;
        var conversation = conversationRepository
                .findById(request.getConversationId())
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        conversation.getParticipants().stream()
                .filter(participantInfo -> userId.equals(participantInfo.getUserId()))
                .findAny()
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        String otherParticipantId = conversation.getParticipants().stream()
                .map(ParticipantInfo::getUserId)
                .filter(id -> !userId.equals(id))
                .findFirst()
                .orElse(null);

        if ("DIRECT".equals(conversation.getType()) && otherParticipantId != null) {
            checkNotBlocked(otherParticipantId);
        }

        boolean isFirstMessageOfRequest = checkMessageRequestGate(conversation, userId);
        // build chat message (get user from profileService)
        var userResponse = profileClient.getProfile(userId);
        if (Objects.isNull(userResponse) || Objects.isNull(userResponse.getResult())) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        var userInfo = userResponse.getResult();

        ChatMessage chatMessage = chatMessageMapper.toChatMessage(request);
        chatMessage.setSender(ParticipantInfo.builder()
                .userId(userInfo.getUserId())
                .username(userInfo.getUsername())
                .firstName(userInfo.getFirstName())
                .lastName(userInfo.getLastName())
                .avatar(userInfo.getAvatar())
                .build());
        chatMessage.setCreatedDate(Instant.now());

        chatMessage = chatMessageRepository.save(chatMessage);

        conversation.setLastMessage(chatMessage.getMessage());
        conversation.setLastMessageAt(chatMessage.getCreatedDate());
        // Có tin nhắn mới tới thì bỏ ẩn hội thoại cho các participant khác đang ẩn nó,
        // tránh họ âm thầm mất tin nhắn.
        conversation.getHiddenBy().removeIf(hiddenUserId -> !hiddenUserId.equals(userId));
        conversationRepository.save(conversation);

        List<String> userIds = conversation.getParticipants().stream()
                .map(ParticipantInfo::getUserId)
                .toList();

        Map<String, WebSocketSession> webSocketSessions = webSocketSessionRepository.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(WebSocketSession::getSocketSessionId, Function.identity()));

        ChatMessageResponse chatMessageResponse = chatMessageMapper.toChatMessageResponse(chatMessage);
        socketIOServer.getAllClients().forEach(client -> {
            var webSocketSession = webSocketSessions.get(client.getSessionId().toString());

            if (Objects.nonNull(webSocketSession)) {
                chatMessageResponse.setMe(webSocketSession.getUserId().equals(userId));
                String message = objectMapper.writeValueAsString(chatMessageResponse);
                client.sendEvent("message", message);
            }
        });

        if (isFirstMessageOfRequest && otherParticipantId != null) {
            pushMessageRequestEvent("MESSAGE_REQUEST_RECEIVED", otherParticipantId, conversation.getId());
        }

        return toChatMessageResponse(chatMessage);
    }

    // Trả về true nếu đây là tin nhắn "mở màn" 1 Message Request mới (conversation
    // đang PENDING và đúng người tạo request đang gửi) — dùng để biết có cần đẩy
    // event MESSAGE_REQUEST_RECEIVED sau khi lưu message hay không.
    private boolean checkMessageRequestGate(Conversation conversation, String userId) {
        if (conversation.getStatus() == ConversationStatus.REJECTED) {
            throw new AppException(ErrorCode.MESSAGE_REQUEST_REJECTED);
        }
        if (conversation.getStatus() != ConversationStatus.PENDING) {
            return false;
        }
        if (!userId.equals(conversation.getRequestedBy())) {
            // mình là người NHẬN request, chưa Accept thì chưa được nhắn lại
            throw new AppException(ErrorCode.MESSAGE_REQUEST_NOT_ACCEPTED);
        }
        long existingMessages = chatMessageRepository.countByConversationId(conversation.getId());
        if (existingMessages >= 1) {
            // người gửi request chỉ được gửi đúng 1 tin nhắn cho tới khi được Accept
            throw new AppException(ErrorCode.MESSAGE_REQUEST_LIMIT_REACHED);
        }
        return true;
    }

    private void pushMessageRequestEvent(String eventType, String targetUserId, String conversationId) {
        Map<String, WebSocketSession> sessionsBySocketId =
                webSocketSessionRepository.findAllByUserIdIn(List.of(targetUserId)).stream()
                        .collect(Collectors.toMap(WebSocketSession::getSocketSessionId, Function.identity()));

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", eventType);
        payload.put("conversationId", conversationId);

        socketIOServer.getAllClients().forEach(client -> {
            if (sessionsBySocketId.containsKey(client.getSessionId().toString())) {
                client.sendEvent("message-request-event", payload);
            }
        });
    }

    private void checkNotBlocked(String otherUserId) {
        var response = friendClient.getFriendshipStatus(otherUserId);
        if (response == null || response.getResult() == null) {
            return;
        }
        String status = response.getResult().getStatus();
        if ("BLOCKED".equals(status) || "BLOCKED_BY".equals(status)) {
            throw new AppException(ErrorCode.BLOCKED);
        }
    }

    private ChatMessageResponse toChatMessageResponse(ChatMessage chatMessage) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        var chatMessageResponse = chatMessageMapper.toChatMessageResponse(chatMessage);
        chatMessageResponse.setMe(userId.equals(chatMessage.getSender().getUserId()));
        return chatMessageResponse;
    }
}
