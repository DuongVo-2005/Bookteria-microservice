package com.devteria.reading.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.devteria.reading.dto.OutboxEventType;
import com.devteria.reading.entity.OutboxEvent;
import com.devteria.reading.repository.OutboxEventRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OutboxEventService {
    OutboxEventRepository outboxEventRepository;
    ObjectMapper objectMapper;

    public void recordEvent(String aggregateId, OutboxEventType eventType, Object payload) {
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Failed to serialize outbox event. aggregateId={}, eventType={}", aggregateId, eventType, e);
            throw new IllegalStateException("Failed to serialize outbox event", e);
        }

        OutboxEvent event = OutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .aggregateType("READING_PROGRESS")
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(jsonPayload)
                .published(false)
                .retryCount(0)
                .createdAt(Instant.now())
                .publishedAt(null)
                .build();
        int maxAttempts = 3;
        for (int attempts = 1; attempts <= maxAttempts; attempts++) {
            try {
                outboxEventRepository.save(event);
                log.debug(
                        "Outbox event recorded successfully. aggregateId={}, eventType={}, attempt={}",
                        aggregateId,
                        eventType,
                        attempts);
                return;
            } catch (Exception e) {
                log.warn(
                        "Failed to save outbox event. aggregateId={}, eventType={}, attempt={}/{}",
                        aggregateId,
                        eventType,
                        attempts,
                        maxAttempts,
                        e);
                if (attempts == maxAttempts) {
                    log.error(
                            "Failed to record outbox event after {} attempts. aggregateId={}, eventType={}",
                            maxAttempts,
                            aggregateId,
                            eventType,
                            e);

                    throw new IllegalStateException("Failed to save outbox event", e);
                }
            }
        }
    }
}
