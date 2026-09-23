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

// idea-spec BA GAP-03: report-service publish "admin-commands" sau khi Admin duyệt report ở
// trạng thái ACTION_TAKEN. Chỉ xử lý message có targetService="group-service", còn lại bỏ qua
// (mọi target-service dùng chung 1 topic, tự lọc theo mình).
//
// Tái dùng thẳng KafkaBookEvent làm envelope (shape giống hệt: eventId/eventType/aggregateId/
// payload-String/timestamp) thay vì tạo thêm 1 DTO trùng lặp — cùng lý do notification-service/
// chat-service tái dùng 1 envelope generic cho nhiều topic khác nhau. application.yaml's
// spring.json.value.default.type chỉ nhận 1 class cho factory mặc định, nên phải cùng class.
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AdminCommandConsumer {
    GroupService groupService;
    IdempotencyService idempotencyService;
    ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "admin-commands", groupId = "group-service")
    public void consume(KafkaBookEvent message) {
        try {
            if (!"ADMIN_COMMAND_EXECUTE".equals(message.getEventType())) {
                return;
            }

            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            if (!"group-service".equals(payload.get("targetService"))) {
                return;
            }

            if (!idempotencyService.markProcessedIfNew(message.getEventId())) {
                log.debug("Duplicate admin-command ignored. eventId={}", message.getEventId());
                return;
            }

            String actionType = (String) payload.get("actionType");
            String targetId = (String) payload.get("targetId");

            if ("HIDE_GROUP".equals(actionType)) {
                groupService.hideGroupBySystem(targetId);
                log.info("[AUDIT] source=report-service action=HIDE_GROUP target={}", targetId);
            } else {
                log.warn("Unknown actionType for group-service: {}", actionType);
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
