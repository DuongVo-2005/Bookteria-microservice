package com.devteria.group.consumer;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.devteria.group.dto.KafkaBookEvent;
import com.devteria.group.service.GroupService;
import com.devteria.group.service.IdempotencyService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

// idea-spec BA OPS-01: identity-service publish "user-events" (USER_LOCKED) khi Admin khoá tài
// khoản. Nếu user đó đang là OWNER của group nào, tự động chuyển quyền cho ADMIN/MEMBER lâu năm
// nhất (xem GroupService.transferOwnershipOnOwnerLockedBySystem). Tái dùng KafkaBookEvent làm
// envelope chung (shape giống hệt AdminCommandConsumer/BookEventConsumer, cùng
// spring.json.value.default.type đã cấu hình sẵn).
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserLockEventConsumer {
    GroupService groupService;
    IdempotencyService idempotencyService;
    ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "user-events", groupId = "group-service")
    public void consume(KafkaBookEvent message) {
        try {
            if (!"USER_LOCKED".equals(message.getEventType())) {
                return;
            }
            if (!idempotencyService.markProcessedIfNew(message.getEventId())) {
                log.debug("Duplicate user-locked event ignored. eventId={}", message.getEventId());
                return;
            }

            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String userId = (String) payload.get("userId");
            if (userId == null || userId.isBlank()) {
                log.warn("user-events USER_LOCKED without userId, ignored. eventId={}", message.getEventId());
                return;
            }

            groupService.transferOwnershipOnOwnerLockedBySystem(userId);
        } catch (Exception e) {
            log.error(
                    "Failed to process user-locked event. aggregateId={}, eventType={}",
                    message.getAggregateId(),
                    message.getEventType(),
                    e);
        }
    }
}
