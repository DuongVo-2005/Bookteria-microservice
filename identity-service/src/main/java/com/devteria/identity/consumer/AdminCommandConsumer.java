package com.devteria.identity.consumer;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.devteria.identity.dto.KafkaAdminCommandEvent;
import com.devteria.identity.service.IdempotencyService;
import com.devteria.identity.service.UserService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

// idea-spec BA GAP-03: report-service publish "admin-commands" sau khi Admin duyệt report ở
// trạng thái ACTION_TAKEN. Chỉ xử lý message có targetService="identity-service".
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AdminCommandConsumer {
    UserService userService;
    IdempotencyService idempotencyService;
    ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "admin-commands", groupId = "identity-service")
    public void consume(KafkaAdminCommandEvent message) {
        try {
            if (!"ADMIN_COMMAND_EXECUTE".equals(message.getEventType())) {
                return;
            }

            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            if (!"identity-service".equals(payload.get("targetService"))) {
                return;
            }

            if (!idempotencyService.markProcessedIfNew(message.getEventId())) {
                log.debug("Duplicate admin-command ignored. eventId={}", message.getEventId());
                return;
            }

            String actionType = (String) payload.get("actionType");
            String targetId = (String) payload.get("targetId");

            if ("LOCK_USER".equals(actionType)) {
                userService.lockUserBySystem(targetId);
            } else {
                log.warn("Unknown actionType for identity-service: {}", actionType);
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
