package com.devteria.notification.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.devteria.notification.dto.event.FriendEventMessage;
import com.devteria.notification.dto.event.FriendRequestEventPayload;
import com.devteria.notification.service.IdempotencyService;
import com.devteria.notification.service.NotificationService;
import com.devteria.notification.service.ProfileLookupService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

// idea-spec Phase 1 - "1. Friend Notification": notification-service consume "friend-events",
// tạo notification in-app khi gửi/chấp nhận lời mời kết bạn. Chỉ xử lý 2 loại event này theo
// đúng yêu cầu - REJECTED/REMOVED/BLOCKED/UNBLOCKED không nằm trong spec, bỏ qua an toàn (default).
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class FriendEventConsumer {
    NotificationService notificationService;
    ProfileLookupService profileLookupService;
    IdempotencyService idempotencyService;

    ObjectMapper objectMapper =
            new ObjectMapper(); // com.fasterxml.jackson (Jackson 2) — khớp value-deserializer JsonDeserializer

    @KafkaListener(topics = "friend-events", containerFactory = "eventEnvelopeKafkaListenerContainerFactory")
    public void consume(FriendEventMessage message) {
        try {
            if (!idempotencyService.markProcessedIfNew(message.getEventId())) {
                log.debug("Duplicate friend event ignored. eventId={}", message.getEventId());
                return;
            }
            switch (message.getEventType()) {
                case "FRIEND_REQUEST_SENT" -> handleRequestSent(message);
                case "FRIEND_REQUEST_ACCEPTED" -> handleRequestAccepted(message);
                default -> {}
            }
        } catch (Exception e) {
            log.error(
                    "Failed to process friend event. aggregateId={}, eventType={}",
                    message.getAggregateId(),
                    message.getEventType(),
                    e);
        }
    }

    private void handleRequestSent(FriendEventMessage message) throws Exception {
        FriendRequestEventPayload payload =
                objectMapper.readValue(message.getPayload(), FriendRequestEventPayload.class);
        String senderName = payload.getSenderUsername() != null ? payload.getSenderUsername() : "Một người dùng";
        notificationService.create(
                payload.getReceiverId(), "Lời mời kết bạn mới", senderName + " đã gửi cho bạn một lời mời kết bạn");
    }

    private void handleRequestAccepted(FriendEventMessage message) throws Exception {
        FriendRequestEventPayload payload =
                objectMapper.readValue(message.getPayload(), FriendRequestEventPayload.class);
        // payload chỉ enrich sẵn senderUsername (người gửi lời mời gốc) - người chấp nhận
        // (receiverId) phải tự resolve qua profile-service.
        String accepterName = profileLookupService.resolveUsername(payload.getReceiverId());
        notificationService.create(
                payload.getSenderId(),
                "Lời mời kết bạn được chấp nhận",
                accepterName + " đã chấp nhận lời mời kết bạn của bạn");
    }
}
