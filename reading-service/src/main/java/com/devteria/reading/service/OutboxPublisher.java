package com.devteria.reading.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.devteria.reading.dto.OutboxEventMessage;
import com.devteria.reading.entity.OutboxEvent;
import com.devteria.reading.repository.OutboxEventRepository;

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
                kafkaTemplate
                        .send("reading-progress-events", event.getAggregateId(), message)
                        .get();
                event.setPublished(true);
                event.setPublishedAt(Instant.now());

                outboxEventRepository.save(event);
            } catch (ExecutionException | InterruptedException e) {
                event.setRetryCount(event.getRetryCount() + 1);
                outboxEventRepository.save(event);
                log.error(
                        "Failed to publish outbox event. id={}, aggregateId={}, retryCount={}",
                        event.getId(),
                        event.getAggregateId(),
                        event.getRetryCount(),
                        e);
            }
        }
    }
}
