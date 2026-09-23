package com.devteria.identity.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.devteria.identity.dto.OutboxEventMessage;
import com.devteria.identity.entity.OutboxEvent;
import com.devteria.identity.repository.OutboxEventRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

// idea-spec BA GAP-01: trước đây welcome-email publish thẳng qua kafkaTemplate.send() — mất luôn
// nếu Kafka down đúng lúc đăng ký. Giờ ghi vào bảng nháp (OutboxEventService.recordEvent(), cùng
// transaction JPA với việc tạo User) rồi job này poll + publish thật, retry nếu lỗi — không còn
// mất event dù Kafka down tạm thời.
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OutboxPublisher {

    OutboxEventRepository outboxEventRepository;
    KafkaTemplate<String, OutboxEventMessage> kafkaTemplate;

    private static final String TOPIC = "notification-delivery";

    @Scheduled(fixedDelay = 5000)
    public void publishEvents() {
        List<OutboxEvent> events = outboxEventRepository.findAllByPublishedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {
            try {
                OutboxEventMessage message = OutboxEventMessage.builder()
                        .eventId(event.getEventId())
                        .eventType(event.getEventType())
                        .aggregateId(event.getAggregateId())
                        .payload(event.getPayload())
                        .timestamp(event.getCreatedAt().toEpochMilli())
                        .build();

                kafkaTemplate.send(TOPIC, event.getAggregateId(), message).get();

                event.setPublished(true);
                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);
            } catch (ExecutionException | InterruptedException e) {
                event.setRetryCount(event.getRetryCount() + 1);
                outboxEventRepository.save(event);

                log.error(
                        "Failed to publish identity outbox event. id={}, aggregateId={}, retryCount={}",
                        event.getId(),
                        event.getAggregateId(),
                        event.getRetryCount(),
                        e);

                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupPublishedEvents() {
        Instant threshold = Instant.now().minus(7, ChronoUnit.DAYS);
        outboxEventRepository.deleteAllByPublishedTrueAndPublishedAtBefore(threshold);
    }
}
