package com.devteria.notification.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.devteria.notification.dto.response.NotificationResponse;
import com.devteria.notification.entity.Notification;
import com.devteria.notification.exception.AppException;
import com.devteria.notification.exception.ErrorCode;
import com.devteria.notification.repository.NotificationRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationService {
    private static final Duration AGGREGATION_WINDOW = Duration.ofMinutes(10);

    NotificationRepository notificationRepository;

    public void create(String userId, String title, String body) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        notificationRepository.save(Notification.builder()
                .userId(userId)
                .title(title)
                .body(body)
                .read(false)
                .createdAt(Instant.now())
                .build());
    }

    // Gộp thông báo (idea-spec Phase 1 - 1.3): nhiều actor cùng tác động lên 1 target trong
    // thời gian ngắn (VD nhiều người comment cùng 1 bài viết nhóm) -> cộng dồn actorCount vào
    // đúng 1 notification UNREAD gần nhất cùng aggregationKey thay vì tạo bản ghi mới mỗi lần.
    // Notification đã đọc rồi thì KHÔNG gộp tiếp - coi như "chu kỳ" mới, tạo bản ghi riêng.
    public void createOrAggregate(
            String userId, String aggregationKey, String title, String singularBody, String pluralBodyTemplate) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        Instant windowStart = Instant.now().minus(AGGREGATION_WINDOW);
        Optional<Notification> existing =
                notificationRepository.findFirstByUserIdAndAggregationKeyAndReadFalseOrderByCreatedAtDesc(
                        userId, aggregationKey);

        if (existing.isPresent() && existing.get().getCreatedAt().isAfter(windowStart)) {
            Notification notification = existing.get();
            int newCount = notification.getActorCount() + 1;
            notification.setActorCount(newCount);
            notification.setTitle(title);
            notification.setBody(pluralBodyTemplate.replace("{count}", String.valueOf(newCount - 1)));
            notification.setCreatedAt(Instant.now());
            notificationRepository.save(notification);
            return;
        }

        notificationRepository.save(Notification.builder()
                .userId(userId)
                .title(title)
                .body(singularBody)
                .read(false)
                .aggregationKey(aggregationKey)
                .actorCount(1)
                .createdAt(Instant.now())
                .build());
    }

    public Page<NotificationResponse> getNotifications(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    public void markAsRead(String userId, String notificationId) {
        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    public void markAllAsRead(String userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalse(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
