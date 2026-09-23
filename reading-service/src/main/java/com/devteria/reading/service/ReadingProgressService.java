package com.devteria.reading.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.devteria.reading.dto.OutboxEventType;
import com.devteria.reading.dto.ReadingProgressStatus;
import com.devteria.reading.dto.request.ReadingProgressUpdateRequest;
import com.devteria.reading.dto.response.ReadingProgressResponse;
import com.devteria.reading.dto.response.ReadingStreakResponse;
import com.devteria.reading.entity.ReadingProgress;
import com.devteria.reading.entity.ReadingStreak;
import com.devteria.reading.mapper.ReadingProgressMapper;
import com.devteria.reading.repository.ReadingProgressRepository;
import com.devteria.reading.repository.ReadingStreakRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReadingProgressService {
    private static final List<Integer> STREAK_BADGE_MILESTONES = List.of(7, 30, 100);

    ReadingProgressRepository readingProgressRepository;
    ReadingProgressMapper readingProgressMapper;
    OutboxEventService outboxEventService;
    ReadingStreakRepository readingStreakRepository;

    public ReadingProgressResponse getProgress(String bookId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        return readingProgressRepository
                .findByUserIdAndBookId(userId, bookId)
                .map(readingProgressMapper::toReadingProgressResponse)
                .orElseGet(() -> ReadingProgressResponse.builder()
                        .bookId(bookId)
                        .currentChapterId(null)
                        .currentChapterIndex(0)
                        .progressPercent(0)
                        .status(ReadingProgressStatus.NOT_STARTED)
                        .lastReadAt(null)
                        .build());
    }

    public List<ReadingProgressResponse> getAllProgress() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        return readingProgressRepository.findByUserIdOrderByLastReadAtDesc(userId).stream()
                .map(readingProgressMapper::toReadingProgressResponse)
                .toList();
    }

    public ReadingProgressResponse updateProgress(String bookId, ReadingProgressUpdateRequest request) {
        return applyProgressUpdate(bookId, request).response();
    }

    // idea-spec BA v2 §3.1 Offline Sync Batch (Progress Monotonicity): tách riêng khỏi
    // updateProgress() để SyncBatchService tái dùng đúng 1 chỗ logic duy nhất khi áp dụng cả loạt
    // write từ hàng đợi offline, có thể không theo đúng thứ tự thời gian thật. Write được coi là
    // "cũ hơn" (stale) nếu clientUpdatedAt của nó xảy ra TRƯỚC lastReadAt đã lưu - bị bỏ qua thay
    // vì ghi đè, tránh 1 write trễ (network chậm/đến sau) làm lùi progress đã mới hơn. Gọi online
    // trực tiếp (clientUpdatedAt = null) luôn coi là "vừa xảy ra" nên luôn thắng, giữ nguyên hành
    // vi cũ - không có rủi ro regression cho updateProgress() hiện có.
    record ProgressApplyResult(ReadingProgressResponse response, boolean applied) {}

    ProgressApplyResult applyProgressUpdate(String bookId, ReadingProgressUpdateRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        ReadingProgress progress = readingProgressRepository
                .findByUserIdAndBookId(userId, bookId)
                .orElseGet(() -> ReadingProgress.builder()
                        .userId(userId)
                        .bookId(bookId)
                        .status(ReadingProgressStatus.NOT_STARTED)
                        .build());

        Instant writeTime = request.getClientUpdatedAt() != null ? request.getClientUpdatedAt() : Instant.now();
        if (progress.getLastReadAt() != null && writeTime.isBefore(progress.getLastReadAt())) {
            return new ProgressApplyResult(readingProgressMapper.toReadingProgressResponse(progress), false);
        }

        ReadingProgressStatus oldStatus = progress.getStatus();
        ReadingProgressStatus newStatus = request.getStatus();

        progress.setCurrentChapterId(request.getCurrentChapterId());
        progress.setCurrentChapterIndex(request.getCurrentChapterIndex());
        progress.setProgressPercent(request.getProgressPercent());
        progress.setStatus(newStatus);
        progress.setLastReadAt(writeTime);

        progress = readingProgressRepository.save(progress);

        recordSyncEventIfChanged(userId, bookId, oldStatus, newStatus);
        touchStreak(userId);

        return new ProgressApplyResult(readingProgressMapper.toReadingProgressResponse(progress), true);
    }

    // idea-spec BA FEAT-02: Daily Reading Streak — xem ghi chú phạm vi ở ReadingStreak entity.
    private void touchStreak(String userId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        ReadingStreak streak = readingStreakRepository
                .findById(userId)
                .orElseGet(() -> ReadingStreak.builder().userId(userId).build());

        if (today.equals(streak.getLastActiveDate())) {
            return; // đã tính hôm nay rồi, tránh cộng nhiều lần cho nhiều lần gọi cùng ngày
        }

        if (today.minusDays(1).equals(streak.getLastActiveDate())) {
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        } else {
            streak.setCurrentStreak(1); // ngày đầu tiên, hoặc đứt mạch (bỏ lỡ >= 1 ngày)
        }
        streak.setLastActiveDate(today);
        streak.setLongestStreak(Math.max(streak.getLongestStreak(), streak.getCurrentStreak()));

        for (int milestone : STREAK_BADGE_MILESTONES) {
            String badge = milestone + "_DAYS";
            if (streak.getCurrentStreak() >= milestone && !streak.getBadges().contains(badge)) {
                streak.getBadges().add(badge);
            }
        }

        readingStreakRepository.save(streak);
    }

    public ReadingStreakResponse getMyStreak() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        return readingStreakRepository
                .findById(userId)
                .map(streak -> ReadingStreakResponse.builder()
                        .currentStreak(streak.getCurrentStreak())
                        .longestStreak(streak.getLongestStreak())
                        .lastActiveDate(streak.getLastActiveDate())
                        .badges(streak.getBadges())
                        .build())
                .orElseGet(() -> ReadingStreakResponse.builder()
                        .currentStreak(0)
                        .longestStreak(0)
                        .badges(List.of())
                        .build());
    }

    // Đồng bộ sang book-service (ReadingList/kệ sách) qua Kafka Outbox — chỉ bắn event khi
    // status THẬT SỰ đổi, tránh spam event mỗi lần user gõ tiến độ (progressPercent) mà status
    // không đổi. Chuyển về NOT_STARTED không đồng bộ (không có UI nào làm việc này).
    private void recordSyncEventIfChanged(
            String userId, String bookId, ReadingProgressStatus oldStatus, ReadingProgressStatus newStatus) {
        if (oldStatus == newStatus) return;

        var payload = Map.of("userId", userId, "bookId", bookId);
        if (newStatus == ReadingProgressStatus.READING) {
            outboxEventService.recordEvent(bookId, OutboxEventType.READING_STARTED, payload);
        } else if (newStatus == ReadingProgressStatus.COMPLETED) {
            outboxEventService.recordEvent(bookId, OutboxEventType.READING_COMPLETED, payload);
        }
    }
}
