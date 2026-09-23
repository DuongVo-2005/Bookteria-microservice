package com.devteria.chat.consumer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.devteria.chat.dto.FriendEventMessage;
import com.devteria.chat.dto.FriendEventPayload;
import com.devteria.chat.entity.WebSocketSession;
import com.devteria.chat.repository.WebSocketSessionRepository;
import com.devteria.chat.service.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class FriendEventConsumer {
    WebSocketSessionRepository webSocketSessionRepository;
    SocketIOServer socketIOServer;
    IdempotencyService idempotencyService;
    ObjectMapper objectMapper =
            new ObjectMapper(); // com.fasterxml.jackson (Jackson 2) — KHÔNG dùng tools.jackson (Jackson 3)

    @KafkaListener(topics = "friend-events", groupId = "chat-service")
    public void consume(FriendEventMessage message) {
        try {
            if (!idempotencyService.markProcessedIfNew(message.getEventId())) {
                log.debug("Duplicate friend event ignored. eventId={}", message.getEventId());
                return;
            }
            FriendEventPayload payload = objectMapper.readValue(message.getPayload(), FriendEventPayload.class);
            List<String> targetUserIds =
                    switch (message.getEventType()) {
                        case "FRIEND_REQUEST_SENT" -> List.of(payload.getReceiverId());
                        case "FRIEND_REQUEST_ACCEPTED", "FRIEND_REQUEST_REJECTED" -> List.of(payload.getSenderId());
                        case "FRIEND_REMOVED" -> List.of(payload.getSenderId(), payload.getReceiverId());
                        case "FRIEND_BLOCKED", "FRIEND_UNBLOCKED" ->
                            List.of(payload.getBlockerId(), payload.getBlockedId());
                        default -> List.of();
                    };
            pushToUsers(targetUserIds, message.getEventType(), payload);
        } catch (Exception e) {
            log.error(
                    "Failed to process friend event. aggregateId={}, eventType={}",
                    message.getAggregateId(),
                    message.getEventType(),
                    e);
        }
    }

    private void pushToUsers(List<String> userIds, String eventType, FriendEventPayload payload) {

        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        Map<String, WebSocketSession> sessionsBySocketId =
                webSocketSessionRepository.findAllByUserIdIn(userIds).stream()
                        .collect(Collectors.toMap(
                                WebSocketSession::getSocketSessionId,
                                Function.identity(),
                                (existing, replacement) -> existing));

        Map<String, Object> notification = new HashMap<>();

        notification.put("eventType", eventType);

        if (payload.getSenderId() != null) {
            notification.put("senderId", payload.getSenderId());
        }

        if (payload.getSenderUsername() != null) {
            notification.put("senderUsername", payload.getSenderUsername());
        }

        if (payload.getSenderAvatar() != null) {
            notification.put("senderAvatar", payload.getSenderAvatar());
        }

        if (payload.getReceiverId() != null) {
            notification.put("receiverId", payload.getReceiverId());
        }

        if (payload.getBlockerId() != null) {
            notification.put("blockerId", payload.getBlockerId());
        }

        if (payload.getBlockedId() != null) {
            notification.put("blockedId", payload.getBlockedId());
        }

        for (SocketIOClient client : socketIOServer.getAllClients()) {

            WebSocketSession session =
                    sessionsBySocketId.get(client.getSessionId().toString());

            if (session != null) {

                client.sendEvent("friend-event", notification);
            }
        }
    }
}
