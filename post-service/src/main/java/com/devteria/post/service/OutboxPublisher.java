package com.devteria.post.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.devteria.post.dto.OutboxEventMessage;
import com.devteria.post.entity.OutboxEvent;
import com.devteria.post.repository.OutboxEventRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OutboxPublisher {
    private static final String TOPIC = "post-events";

    OutboxEventRepository outboxEventRepository;
    KafkaTemplate<String, OutboxEventMessage> kafkaTemplate;

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
                        "Failed to publish post event. id={}, aggregateId={}, retryCount={}",
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

    // idea-spec Phase 2 - "4.1 Outbox Cleanup Job" chỉ nói rõ friend-service/group-service,
    // nhưng thêm luôn ở đây từ ngày đầu vì post-service cũng có Outbox từ Phase 3 - rẻ hơn nhiều
    // so với quay lại vá sau (đã áp dụng bài học thật từ Phase 2).
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupPublishedEvents() {
        Instant threshold = Instant.now().minus(7, ChronoUnit.DAYS);
        outboxEventRepository.deleteAllByPublishedTrueAndPublishedAtBefore(threshold);
    }
}
