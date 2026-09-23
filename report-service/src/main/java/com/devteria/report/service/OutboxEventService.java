package com.devteria.report.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.devteria.report.dto.OutboxEventType;
import com.devteria.report.entity.OutboxEvent;
import com.devteria.report.repository.OutboxEventRepository;

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
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(jsonPayload)
                .published(false)
                .retryCount(0)
                .createdAt(Instant.now())
                .publishedAt(null)
                .build();

        outboxEventRepository.save(event);
    }
}
