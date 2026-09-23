package com.devteria.report.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.devteria.report.dto.OutboxEventMessage;
import com.devteria.report.entity.OutboxEvent;
import com.devteria.report.repository.OutboxEventRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

// idea-spec BA GAP-03: publish "admin-commands" qua Outbox thay vì gọi thẳng kafkaTemplate.send()
// — 1 lệnh moderation bị mất do Kafka down là bug nghiêm trọng hơn nhiều so với welcome email mất
// (GAP-01), nên áp dụng đúng pattern reliability đã proven xuyên suốt dự án ngay từ đầu.
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OutboxPublisher {
    OutboxEventRepository outboxEventRepository;
    KafkaTemplate<String, OutboxEventMessage> kafkaTemplate;

    private static final String TOPIC = "admin-commands";

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
                        "Failed to publish admin-command event. id={}, aggregateId={}, retryCount={}",
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
