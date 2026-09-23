package com.devteria.chat.consumer;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.devteria.chat.dto.FriendEventMessage;
import com.devteria.chat.dto.UserLockedEventPayload;
import com.devteria.chat.entity.WebSocketSession;
import com.devteria.chat.repository.WebSocketSessionRepository;
import com.devteria.chat.service.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

// idea-spec Phase 4 - "11.1 Session Revocation": identity-service publish "user-events" khi
// Admin khoá tài khoản (REST đã tự chặn qua introspect() - xem be-report.md - nhưng WebSocket
// đang mở không tự re-check per-message, cần tín hiệu riêng để đóng thật). Tái dùng đúng envelope
// FriendEventMessage (đã có default-type cấu hình sẵn ở application.yaml, không cần factory
// riêng) + IdempotencyService đã có.
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserLockEventConsumer {
    WebSocketSessionRepository webSocketSessionRepository;
    SocketIOServer socketIOServer;
    IdempotencyService idempotencyService;
    ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "user-events", groupId = "chat-service")
    public void consume(FriendEventMessage message) {
        try {
            if (!idempotencyService.markProcessedIfNew(message.getEventId())) {
                log.debug("Duplicate user event ignored. eventId={}", message.getEventId());
                return;
            }
            // idea-spec BA v2 §2.2: deactivate cũng cần ngắt WebSocket đang mở, cùng lý do và
            // cùng payload shape với USER_LOCKED (chỉ có userId).
            if (!"USER_LOCKED".equals(message.getEventType()) && !"USER_DEACTIVATED".equals(message.getEventType())) {
                return;
            }
            UserLockedEventPayload payload = objectMapper.readValue(message.getPayload(), UserLockedEventPayload.class);
            disconnectUser(payload.getUserId());
        } catch (Exception e) {
            log.error(
                    "Failed to process user event. aggregateId={}, eventType={}",
                    message.getAggregateId(),
                    message.getEventType(),
                    e);
        }
    }

    private void disconnectUser(String userId) {
        if (userId == null) {
            return;
        }
        List<WebSocketSession> sessions = webSocketSessionRepository.findAllByUserIdIn(List.of(userId));
        Set<String> socketSessionIds =
                sessions.stream().map(WebSocketSession::getSocketSessionId).collect(Collectors.toSet());

        if (socketSessionIds.isEmpty()) {
            return;
        }

        for (SocketIOClient client : socketIOServer.getAllClients()) {
            if (socketSessionIds.contains(client.getSessionId().toString())) {
                client.disconnect();
            }
        }
    }
}
