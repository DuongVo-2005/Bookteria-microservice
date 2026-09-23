package com.devteria.notification.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.devteria.event.dto.NotificationEvent;
import com.devteria.notification.dto.event.FriendEventMessage;
import com.devteria.notification.dto.request.Recipient;
import com.devteria.notification.dto.request.SendEmailRequest;
import com.devteria.notification.service.EmailService;
import com.devteria.notification.service.IdempotencyService;
import com.devteria.notification.service.NotificationService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

// idea-spec BA GAP-01: identity-service đổi từ publish thẳng "notification-delivery" (type
// NotificationEvent trần) sang Outbox pattern (envelope chuẩn eventId/eventType/aggregateId/
// payload-String/timestamp, giống hệt friend-events/group-events/post-events) — consumer cũ
// (NotificationController.listenNotificationDelivery, nhận thẳng NotificationEvent qua factory
// mặc định) không còn đúng shape nữa, thay bằng consumer này, dùng chung
// eventEnvelopeKafkaListenerContainerFactory + idempotency đã có sẵn từ Phase 1/2 (trước đây
// notification-delivery KHÔNG có idempotency riêng — giờ có, tránh gửi trùng email/notification
// nếu OutboxPublisher retry redeliver).
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class WelcomeEmailConsumer {
    EmailService emailService;
    NotificationService notificationService;
    IdempotencyService idempotencyService;
    ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(
            topics = "notification-delivery",
            groupId = "notification-group",
            containerFactory = "eventEnvelopeKafkaListenerContainerFactory")
    public void consume(FriendEventMessage message) {
        try {
            if (!idempotencyService.markProcessedIfNew(message.getEventId())) {
                log.debug("Duplicate welcome-email event ignored. eventId={}", message.getEventId());
                return;
            }

            NotificationEvent event = objectMapper.readValue(message.getPayload(), NotificationEvent.class);

            emailService.sendEmail(SendEmailRequest.builder()
                    .to(Recipient.builder().email(event.getRecipient()).build())
                    .subject(event.getSubject())
                    .htmlContent(event.getBody())
                    .build());

            // Đồng thời lưu bản in-app song song với gửi email — không thay thế kênh email,
            // chỉ thêm để notification center trên FE có dữ liệu để hiển thị.
            notificationService.create(event.getUserId(), event.getSubject(), event.getBody());
        } catch (Exception e) {
            log.error(
                    "Failed to process welcome-email event. aggregateId={}, eventType={}",
                    message.getAggregateId(),
                    message.getEventType(),
                    e);
        }
    }
}
