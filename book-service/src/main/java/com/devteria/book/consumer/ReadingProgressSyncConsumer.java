package com.devteria.book.consumer;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.devteria.book.dto.ReadingStatus;
import com.devteria.book.dto.ReadingSyncEventMessage;
import com.devteria.book.service.IdempotencyService;
import com.devteria.book.service.ReadingListService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

// Nghe topic "reading-progress-events" (Outbox, reading-service phát ra khi user bắt đầu/hoàn
// thành đọc 1 sách) -> tự cập nhật ReadingList (kệ sách) sang READING/COMPLETED. Đây là consumer
// Kafka ĐẦU TIÊN của book-service (trước đây chỉ produce) — bọc đủ idempotency + try/catch quanh
// toàn bộ message, đúng pattern FriendEventConsumer/GroupEventConsumer bên chat-service.
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ReadingProgressSyncConsumer {
    ReadingListService readingListService;
    IdempotencyService idempotencyService;

    // ObjectMapper tự khởi tạo (Jackson 2 thuần), KHÔNG inject qua constructor — bug-class đã
    // biết của dự án: Spring chỉ autoconfigure bean ObjectMapper của Jackson 3 (tools.jackson),
    // inject qua constructor sẽ NoSuchBeanDefinitionException/UnsatisfiedDependencyException.
    ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "reading-progress-events", groupId = "book-service")
    public void consume(ReadingSyncEventMessage message) {
        if (!idempotencyService.markProcessedIfNew(message.getEventId())) {
            log.debug("Skip duplicate reading-progress-event, eventId={}", message.getEventId());
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, String> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String userId = payload.get("userId");
            String bookId = payload.get("bookId");

            ReadingStatus newStatus = "READING_COMPLETED".equals(message.getEventType())
                    ? ReadingStatus.COMPLETED
                    : ReadingStatus.READING;

            readingListService.syncStatusFromReadingProgress(userId, bookId, newStatus);
        } catch (Exception e) {
            log.error(
                    "Failed to process reading-progress-event, aggregateId={}, eventType={}",
                    message.getAggregateId(),
                    message.getEventType(),
                    e);
        }
    }
}
