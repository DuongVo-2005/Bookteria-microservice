package com.devteria.notification.consumer;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.devteria.notification.dto.event.FriendEventMessage;
import com.devteria.notification.dto.event.GroupEventPayload;
import com.devteria.notification.service.IdempotencyService;
import com.devteria.notification.service.NotificationService;
import com.devteria.notification.service.ProfileLookupService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

// idea-spec Phase 1 - "2. Group Notification": notification-service consume "group-events".
// GROUP_MEMBER_JOINED/GROUP_COMMENT_CREATED dùng createOrAggregate() (mục 1.3 "gộp thông báo")
// vì đây là 2 event nhiều actor có thể tác động lên cùng 1 target (group/post) trong thời gian
// ngắn - ví dụ thật gần nhất với "nhiều người Like 1 bài viết" trong đề bài gốc, vốn KHÔNG áp
// dụng được nguyên văn vì group-service hiện chưa publish event nào cho hành động Like
// (toggleLike() không gọi outboxEventService — xem be-report.md mục Known Issues).
// GROUP_MEMBER_LEFT/GROUP_POST_DELETED cố tình KHÔNG tạo notification (quyết định sản phẩm nhỏ,
// xem be-report.md) - không nằm trong yêu cầu rõ ràng của idea-spec.
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class GroupEventConsumer {
    NotificationService notificationService;
    ProfileLookupService profileLookupService;
    IdempotencyService idempotencyService;

    ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "group-events", containerFactory = "eventEnvelopeKafkaListenerContainerFactory")
    public void consume(FriendEventMessage message) {
        try {
            if (!idempotencyService.markProcessedIfNew(message.getEventId())) {
                log.debug("Duplicate group event ignored. eventId={}", message.getEventId());
                return;
            }
            GroupEventPayload payload = objectMapper.readValue(message.getPayload(), GroupEventPayload.class);
            switch (message.getEventType()) {
                case "GROUP_POST_CREATED" -> handlePostCreated(payload);
                case "GROUP_MEMBER_JOINED" -> handleMemberJoined(payload);
                case "GROUP_COMMENT_CREATED" -> handleCommentCreated(payload);
                case "GROUP_ROLE_CHANGED" -> handleRoleChanged(payload);
                default -> {}
            }
        } catch (Exception e) {
            log.error(
                    "Failed to process group event. aggregateId={}, eventType={}",
                    message.getAggregateId(),
                    message.getEventType(),
                    e);
        }
    }

    private void handlePostCreated(GroupEventPayload payload) {
        String groupLabel = groupLabel(payload);
        forEachTarget(
                payload,
                userId -> notificationService.create(
                        userId, "Bài đăng mới trong nhóm", "Có bài đăng mới trong nhóm " + groupLabel));
    }

    private void handleMemberJoined(GroupEventPayload payload) {
        String groupLabel = groupLabel(payload);
        String actorName = profileLookupService.resolveUsername(payload.getUserId());
        String aggregationKey = "group-joined:" + payload.getGroupId();
        forEachTarget(
                payload,
                userId -> notificationService.createOrAggregate(
                        userId,
                        aggregationKey,
                        "Thành viên mới",
                        actorName + " đã tham gia nhóm " + groupLabel,
                        actorName + " và {count} người khác đã tham gia nhóm " + groupLabel));
    }

    private void handleCommentCreated(GroupEventPayload payload) {
        String groupLabel = groupLabel(payload);
        String actorName = profileLookupService.resolveUsername(payload.getActorId());
        String aggregationKey = "group-comment:" + payload.getPostId();
        forEachTarget(
                payload,
                userId -> notificationService.createOrAggregate(
                        userId,
                        aggregationKey,
                        "Bình luận mới",
                        actorName + " đã bình luận bài đăng trong nhóm " + groupLabel,
                        actorName + " và {count} người khác đã bình luận bài đăng trong nhóm " + groupLabel));
    }

    private void handleRoleChanged(GroupEventPayload payload) {
        String groupLabel = groupLabel(payload);
        notificationService.create(
                payload.getUserId(),
                "Vai trò trong nhóm đã đổi",
                "Vai trò của bạn trong nhóm " + groupLabel + " đã đổi thành " + payload.getRole());
    }

    private void forEachTarget(GroupEventPayload payload, java.util.function.Consumer<String> action) {
        List<String> targets = payload.getTargetUserIds();
        if (targets == null) {
            return;
        }
        targets.forEach(action);
    }

    private String groupLabel(GroupEventPayload payload) {
        return payload.getGroupName() != null ? payload.getGroupName() : payload.getGroupId();
    }
}
