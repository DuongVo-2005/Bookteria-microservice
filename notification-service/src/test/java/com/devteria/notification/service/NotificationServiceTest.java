package com.devteria.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devteria.notification.entity.Notification;
import com.devteria.notification.repository.NotificationRepository;

// Unit test thuần Mockito cho NotificationService.createOrAggregate() (idea-spec Phase 1 - 1.3
// "Gộp thông báo") — logic dễ sai nhất: đếm actorCount, thay {count}, và ranh giới "đã đọc thì
// không gộp tiếp / hết cửa sổ thời gian thì không gộp tiếp".
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final String USER_ID = "user-1";
    private static final String AGGREGATION_KEY = "group-comment:post-1";

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
    }

    @Test
    void createOrAggregate_firstCall_createsNewNotificationWithSingularBody() {
        when(notificationRepository.findFirstByUserIdAndAggregationKeyAndReadFalseOrderByCreatedAtDesc(
                        USER_ID, AGGREGATION_KEY))
                .thenReturn(Optional.empty());

        notificationService.createOrAggregate(
                USER_ID, AGGREGATION_KEY, "Bình luận mới", "A đã bình luận", "A và {count} người khác đã bình luận");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertThat(saved.getBody()).isEqualTo("A đã bình luận");
        assertThat(saved.getActorCount()).isEqualTo(1);
        assertThat(saved.getAggregationKey()).isEqualTo(AGGREGATION_KEY);
    }

    @Test
    void createOrAggregate_secondCallWithinWindowAndUnread_incrementsCountAndUsesPluralBody() {
        Notification existing = Notification.builder()
                .userId(USER_ID)
                .aggregationKey(AGGREGATION_KEY)
                .actorCount(1)
                .read(false)
                .body("A đã bình luận")
                .createdAt(Instant.now().minusSeconds(30))
                .build();
        when(notificationRepository.findFirstByUserIdAndAggregationKeyAndReadFalseOrderByCreatedAtDesc(
                        USER_ID, AGGREGATION_KEY))
                .thenReturn(Optional.of(existing));

        notificationService.createOrAggregate(
                USER_ID, AGGREGATION_KEY, "Bình luận mới", "B đã bình luận", "B và {count} người khác đã bình luận");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        // actorCount tăng lên 2 (A + B), {count} thay bằng "1" (số người KHÁC ngoài actor mới nhất B).
        assertThat(saved.getActorCount()).isEqualTo(2);
        assertThat(saved.getBody()).isEqualTo("B và 1 người khác đã bình luận");
    }

    @Test
    void createOrAggregate_existingButOutsideWindow_createsNewSeparateNotification() {
        Notification staleNotification = Notification.builder()
                .userId(USER_ID)
                .aggregationKey(AGGREGATION_KEY)
                .actorCount(3)
                .read(false)
                .createdAt(Instant.now().minusSeconds(20 * 60)) // 20 phút trước, ngoài cửa sổ 10 phút
                .build();
        when(notificationRepository.findFirstByUserIdAndAggregationKeyAndReadFalseOrderByCreatedAtDesc(
                        USER_ID, AGGREGATION_KEY))
                .thenReturn(Optional.of(staleNotification));

        notificationService.createOrAggregate(
                USER_ID, AGGREGATION_KEY, "Bình luận mới", "C đã bình luận", "C và {count} người khác đã bình luận");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertThat(saved.getActorCount()).isEqualTo(1);
        assertThat(saved.getBody()).isEqualTo("C đã bình luận");
    }

    @Test
    void createOrAggregate_existingButAlreadyRead_isNeverConsideredSinceQueryFiltersReadFalse() {
        // findFirstByUserIdAndAggregationKeyAndReadFalseOrderByCreatedAtDesc tự lọc read=false ở
        // tầng query - notification đã đọc sẽ không bao giờ được trả về, luôn tạo mới ở tầng service.
        when(notificationRepository.findFirstByUserIdAndAggregationKeyAndReadFalseOrderByCreatedAtDesc(
                        USER_ID, AGGREGATION_KEY))
                .thenReturn(Optional.empty());

        notificationService.createOrAggregate(
                USER_ID, AGGREGATION_KEY, "Bình luận mới", "D đã bình luận", "D và {count} người khác đã bình luận");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getActorCount()).isEqualTo(1);
    }

    @Test
    void createOrAggregate_blankUserId_doesNothing() {
        notificationService.createOrAggregate(" ", AGGREGATION_KEY, "t", "s", "p {count}");

        verify(notificationRepository, times(0)).save(any());
    }
}
