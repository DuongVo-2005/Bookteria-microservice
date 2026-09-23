package com.devteria.book.consumer;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.devteria.book.dto.ReadingSyncEventMessage;
import com.devteria.book.service.IdempotencyService;
import com.devteria.book.service.ReviewService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

// idea-spec BA GAP-03: report-service publish "admin-commands" sau khi Admin duyệt report ở
// trạng thái ACTION_TAKEN. Chỉ xử lý message có targetService="book-service".
//
// Tái dùng ReadingSyncEventMessage làm envelope (shape giống hệt generic
// eventId/eventType/aggregateId/payload/timestamp) — book-service's factory mặc định chỉ nhận 1
// spring.json.value.default.type, không thể khai thêm class thứ 2 cho cùng factory.
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AdminCommandConsumer {
    ReviewService reviewService;
    IdempotencyService idempotencyService;
    ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "admin-commands", groupId = "book-service")
    public void consume(ReadingSyncEventMessage message) {
        try {
            if (!"ADMIN_COMMAND_EXECUTE".equals(message.getEventType())) {
                return;
            }

            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            if (!"book-service".equals(payload.get("targetService"))) {
                return;
            }

            if (!idempotencyService.markProcessedIfNew(message.getEventId())) {
                log.debug("Duplicate admin-command ignored. eventId={}", message.getEventId());
                return;
            }

            String actionType = (String) payload.get("actionType");
            String targetId = (String) payload.get("targetId");

            if ("REMOVE_REVIEW".equals(actionType)) {
                reviewService.removeReviewBySystem(targetId);
                log.info("[AUDIT] source=report-service action=REMOVE_REVIEW target={}", targetId);
            } else {
                log.warn("Unknown actionType for book-service: {}", actionType);
            }
        } catch (Exception e) {
            log.error(
                    "Failed to process admin-command event. aggregateId={}, eventType={}",
                    message.getAggregateId(),
                    message.getEventType(),
                    e);
        }
    }
}
