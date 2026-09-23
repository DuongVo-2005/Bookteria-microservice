package com.devteria.post.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.devteria.post.dto.OutboxEventType;
import com.devteria.post.entity.OutboxEvent;
import com.devteria.post.repository.OutboxEventRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

// idea-spec Phase 3 - "10.1 Rich Text & Mention/Hashtag" (bắn notification khi mention) —
// post-service lần đầu cần publish Kafka event, copy đúng Outbox pattern đã có ở
// friend-service/group-service/book-service, không tạo pattern mới.
//
// idea-spec Phase 6 - "22.1 Search Integration" - FIX bug thật: field này trước đây tự
// "new ObjectMapper()" (com.fasterxml.jackson.databind, Jackson 2, KHÔNG có module JSR310) thay
// vì inject bean. Không lỗi cho tới khi PostIndexEventPayload (POST_CREATED) thêm field
// Instant createdDate - Jackson 2 không tự serialize Instant nếu thiếu jackson-datatype-jsr310,
// ném InvalidDefinitionException, làm createPost() fail 500 toàn bộ (không chỉ index bị bỏ lỡ).
// Đổi sang inject tools.jackson.databind.ObjectMapper (Jackson 3, bean auto-config của Spring
// Boot 4.1.1, hỗ trợ Instant sẵn không cần module) - đúng pattern đã proven ở book-service's
// OutboxEventService (BookResponse cũng có Instant, chưa từng lỗi).
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
                .aggregateType("POST")
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
