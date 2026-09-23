package com.devteria.post.consumer;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.devteria.post.dto.KafkaAdminCommandEvent;
import com.devteria.post.service.IdempotencyService;
import com.devteria.post.service.PostService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

// idea-spec BA GAP-03: report-service publish "admin-commands" khi Admin duyệt report ở trạng
// thái ACTION_TAKEN — trước đây không có bất kỳ lệnh nào gửi tới service sở hữu nội dung
// (report-service hoạt động biệt lập, chỉ lưu report). Consumer này lắng nghe TOÀN BỘ
// admin-commands nhưng chỉ xử lý message có targetService="post-service" (mọi service khác đọc
// cùng topic, tự lọc theo targetService của mình — đơn giản hơn nhiều topic riêng cho 1 tính năng
// tần suất thấp). Consumer Kafka đầu tiên của post-service.
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AdminCommandConsumer {
    PostService postService;
    IdempotencyService idempotencyService;
    ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "admin-commands", groupId = "post-service")
    public void consume(KafkaAdminCommandEvent message) {
        try {
            if (!"ADMIN_COMMAND_EXECUTE".equals(message.getEventType())) {
                return;
            }

            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            if (!"post-service".equals(payload.get("targetService"))) {
                return; // lệnh dành cho service khác, bỏ qua
            }

            if (!idempotencyService.markProcessedIfNew(message.getEventId())) {
                log.debug("Duplicate admin-command ignored. eventId={}", message.getEventId());
                return;
            }

            String actionType = (String) payload.get("actionType");
            String targetId = (String) payload.get("targetId");

            switch (actionType) {
                case "DELETE_POST" -> postService.deletePostBySystem(targetId);
                case "DELETE_COMMENT" -> postService.deleteCommentBySystem(targetId);
                default -> log.warn("Unknown actionType for post-service: {}", actionType);
            }

            log.info("[AUDIT] source=report-service action={} target={}", actionType, targetId);
        } catch (Exception e) {
            log.error(
                    "Failed to process admin-command event. aggregateId={}, eventType={}",
                    message.getAggregateId(),
                    message.getEventType(),
                    e);
        }
    }
}
