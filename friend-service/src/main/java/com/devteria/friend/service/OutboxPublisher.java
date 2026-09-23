package com.devteria.friend.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.devteria.friend.dto.OutboxEventMessage;
import com.devteria.friend.entity.OutboxEvent;
import com.devteria.friend.repository.OutboxEventRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OutboxPublisher {

    OutboxEventRepository outboxEventRepository;

    KafkaTemplate<String, OutboxEventMessage> kafkaTemplate;

    private static final String TOPIC = "friend-events";

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
                        "Failed to publish friend event. id={}, aggregateId={}, retryCount={}",
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

    // idea-spec Phase 2 - "4.1 Outbox Cleanup Job": dọn record đã publish thành công quá 7
    // ngày, tránh outbox_events phình vô hạn. Chỉ xoá published=true - record chưa publish
    // (published=false) không bao giờ bị đụng tới dù cũ, tránh mất event chưa gửi được.
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupPublishedEvents() {
        Instant threshold = Instant.now().minus(7, ChronoUnit.DAYS);
        outboxEventRepository.deleteAllByPublishedTrueAndPublishedAtBefore(threshold);
    }
}
