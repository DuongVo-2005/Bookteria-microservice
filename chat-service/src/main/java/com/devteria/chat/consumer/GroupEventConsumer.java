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
import com.devteria.chat.dto.GroupEventPayload;
import com.devteria.chat.entity.WebSocketSession;
import com.devteria.chat.repository.WebSocketSessionRepository;
import com.devteria.chat.service.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

// Tái dùng FriendEventMessage làm envelope chung (eventType/aggregateId/payload) vì
// group-service publish đúng cùng hình dạng — tránh phải khai báo default-type riêng
// cho listener này trong application.yaml (spring.json.value.default.type chỉ nhận 1 giá trị).
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class GroupEventConsumer {
    WebSocketSessionRepository webSocketSessionRepository;
    SocketIOServer socketIOServer;
    IdempotencyService idempotencyService;
    ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "group-events", groupId = "chat-service")
    public void consume(FriendEventMessage message) {
        try {
            if (!idempotencyService.markProcessedIfNew(message.getEventId())) {
                log.debug("Duplicate group event ignored. eventId={}", message.getEventId());
                return;
            }
            GroupEventPayload payload = objectMapper.readValue(message.getPayload(), GroupEventPayload.class);
            List<String> targetUserIds = payload.getTargetUserIds();
            pushToUsers(targetUserIds, message.getEventType(), payload);
        } catch (Exception e) {
            log.error(
                    "Failed to process group event. aggregateId={}, eventType={}",
                    message.getAggregateId(),
                    message.getEventType(),
                    e);
        }
    }

    private void pushToUsers(List<String> userIds, String eventType, GroupEventPayload payload) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        Map<String, WebSocketSession> sessionsBySocketId =
                webSocketSessionRepository.findAllByUserIdIn(userIds).stream()
                        .collect(Collectors.toMap(
                                WebSocketSession::getSocketSessionId,
                                Function.identity(),
                                (existing, replacement) -> existing));

        if (sessionsBySocketId.isEmpty()) {
            return;
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("eventType", eventType);
        notification.put("groupId", payload.getGroupId());
        if (payload.getGroupName() != null) notification.put("groupName", payload.getGroupName());
        if (payload.getUserId() != null) notification.put("userId", payload.getUserId());
        if (payload.getActorId() != null) notification.put("actorId", payload.getActorId());
        if (payload.getRole() != null) notification.put("role", payload.getRole());
        if (payload.getPostId() != null) notification.put("postId", payload.getPostId());
        if (payload.getCommentId() != null) notification.put("commentId", payload.getCommentId());

        for (SocketIOClient client : socketIOServer.getAllClients()) {
            WebSocketSession session =
                    sessionsBySocketId.get(client.getSessionId().toString());
            if (session != null) {
                client.sendEvent("group-event", notification);
            }
        }
    }
}
