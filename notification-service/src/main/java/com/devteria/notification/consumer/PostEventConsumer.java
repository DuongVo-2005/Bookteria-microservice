package com.devteria.notification.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.devteria.notification.dto.event.FriendEventMessage;
import com.devteria.notification.dto.event.PostMentionEventPayload;
import com.devteria.notification.service.IdempotencyService;
import com.devteria.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

// idea-spec Phase 3 - "10.1 Rich Text & Mention": consume "post-events" (mới, post-service
// lần đầu publish) - tạo notification cho mỗi user bị mention. Tái dùng đúng
// eventEnvelopeKafkaListenerContainerFactory đã có từ Phase 1 (cùng envelope FriendEventMessage,
// cùng ErrorHandlingDeserializer + DLT) và IdempotencyService đã có từ Phase 2 - không tạo
// hạ tầng Kafka mới.
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PostEventConsumer {
    NotificationService notificationService;
    IdempotencyService idempotencyService;

    ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "post-events", containerFactory = "eventEnvelopeKafkaListenerContainerFactory")
    public void consume(FriendEventMessage message) {
        try {
            if (!idempotencyService.markProcessedIfNew(message.getEventId())) {
                log.debug("Duplicate post event ignored. eventId={}", message.getEventId());
                return;
            }
            if (!"POST_MENTIONED".equals(message.getEventType())) {
                return;
            }
            PostMentionEventPayload payload =
                    objectMapper.readValue(message.getPayload(), PostMentionEventPayload.class);
            String authorName = payload.getAuthorUsername() != null ? payload.getAuthorUsername() : "Một người dùng";
            if (payload.getMentionedUserIds() == null) {
                return;
            }
            for (String userId : payload.getMentionedUserIds()) {
                notificationService.create(
                        userId, "Bạn được nhắc đến", authorName + " đã nhắc đến bạn trong một bài viết");
            }
        } catch (Exception e) {
            log.error(
                    "Failed to process post event. aggregateId={}, eventType={}",
                    message.getAggregateId(),
                    message.getEventType(),
                    e);
        }
    }
}
