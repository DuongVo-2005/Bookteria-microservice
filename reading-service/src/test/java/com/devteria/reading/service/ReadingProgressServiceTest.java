package com.devteria.reading.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.devteria.reading.dto.ReadingProgressStatus;
import com.devteria.reading.dto.request.ReadingProgressUpdateRequest;
import com.devteria.reading.dto.response.ReadingProgressResponse;
import com.devteria.reading.dto.response.ReadingStreakResponse;
import com.devteria.reading.entity.ReadingProgress;
import com.devteria.reading.entity.ReadingStreak;
import com.devteria.reading.mapper.ReadingProgressMapper;
import com.devteria.reading.repository.ReadingProgressRepository;
import com.devteria.reading.repository.ReadingStreakRepository;

// Unit test thuần Mockito cho ReadingProgressService — chưa từng có test nào trong
// reading-service trước đây. Tập trung vào touchStreak() (FEAT-02 Daily Reading Streak), logic
// phức tạp nhất và rủi ro nhất (đếm ngày liên tục + badge milestone), gọi gián tiếp qua
// updateProgress() vì touchStreak() là private.
@ExtendWith(MockitoExtension.class)
class ReadingProgressServiceTest {

    private static final String USER_ID = "user-1";
    private static final String BOOK_ID = "book-1";

    @Mock
    private ReadingProgressRepository readingProgressRepository;

    @Mock
    private ReadingProgressMapper readingProgressMapper;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private ReadingStreakRepository readingStreakRepository;

    private ReadingProgressService readingProgressService;

    @BeforeEach
    void setUp() {
        readingProgressService = new ReadingProgressService(
                readingProgressRepository, readingProgressMapper, outboxEventService, readingStreakRepository);

        SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(USER_ID);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // lenient: chỉ dùng ở các test gọi updateProgress(), test gọi getMyStreak() không đụng tới.
        org.mockito.Mockito.lenient()
                .when(readingProgressRepository.findByUserIdAndBookId(USER_ID, BOOK_ID))
                .thenReturn(Optional.empty());
        org.mockito.Mockito.lenient()
                .when(readingProgressRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.lenient()
                .when(readingProgressMapper.toReadingProgressResponse(any()))
                .thenReturn(ReadingProgressResponse.builder().bookId(BOOK_ID).build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private ReadingProgressUpdateRequest request(ReadingProgressStatus status) {
        return ReadingProgressUpdateRequest.builder()
                .currentChapterId("chapter-1")
                .currentChapterIndex(1)
                .progressPercent(50)
                .status(status)
                .build();
    }

    @Test
    void updateProgress_firstEverActivity_startsStreakAtOne() {
        when(readingStreakRepository.findById(USER_ID)).thenReturn(Optional.empty());

        readingProgressService.updateProgress(BOOK_ID, request(ReadingProgressStatus.READING));

        ArgumentCaptor<ReadingStreak> captor = ArgumentCaptor.forClass(ReadingStreak.class);
        org.mockito.Mockito.verify(readingStreakRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentStreak()).isEqualTo(1);
        assertThat(captor.getValue().getLongestStreak()).isEqualTo(1);
        assertThat(captor.getValue().getLastActiveDate()).isEqualTo(LocalDate.now(ZoneOffset.UTC));
    }

    @Test
    void updateProgress_consecutiveDay_incrementsStreak() {
        ReadingStreak existing = ReadingStreak.builder()
                .userId(USER_ID)
                .currentStreak(6)
                .longestStreak(6)
                .lastActiveDate(LocalDate.now(ZoneOffset.UTC).minusDays(1))
                .build();
        when(readingStreakRepository.findById(USER_ID)).thenReturn(Optional.of(existing));

        readingProgressService.updateProgress(BOOK_ID, request(ReadingProgressStatus.READING));

        ArgumentCaptor<ReadingStreak> captor = ArgumentCaptor.forClass(ReadingStreak.class);
        org.mockito.Mockito.verify(readingStreakRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentStreak()).isEqualTo(7);
        assertThat(captor.getValue().getBadges()).contains("7_DAYS");
    }

    @Test
    void updateProgress_gapOfMoreThanOneDay_resetsStreakToOne() {
        ReadingStreak existing = ReadingStreak.builder()
                .userId(USER_ID)
                .currentStreak(15)
                .longestStreak(15)
                .lastActiveDate(LocalDate.now(ZoneOffset.UTC).minusDays(3))
                .build();
        when(readingStreakRepository.findById(USER_ID)).thenReturn(Optional.of(existing));

        readingProgressService.updateProgress(BOOK_ID, request(ReadingProgressStatus.READING));

        ArgumentCaptor<ReadingStreak> captor = ArgumentCaptor.forClass(ReadingStreak.class);
        org.mockito.Mockito.verify(readingStreakRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentStreak()).isEqualTo(1);
        // longestStreak vẫn giữ mốc cũ, không bị reset theo currentStreak.
        assertThat(captor.getValue().getLongestStreak()).isEqualTo(15);
    }

    @Test
    void updateProgress_sameDayCalledTwice_doesNotDoubleCountStreak() {
        ReadingStreak existing = ReadingStreak.builder()
                .userId(USER_ID)
                .currentStreak(3)
                .longestStreak(3)
                .lastActiveDate(LocalDate.now(ZoneOffset.UTC))
                .build();
        when(readingStreakRepository.findById(USER_ID)).thenReturn(Optional.of(existing));

        readingProgressService.updateProgress(BOOK_ID, request(ReadingProgressStatus.READING));

        org.mockito.Mockito.verify(readingStreakRepository, org.mockito.Mockito.never())
                .save(any());
    }

    @Test
    void updateProgress_crossingThirtyDayMilestone_awardsBothMilestoneBadges() {
        // Nhảy thẳng từ streak=6 (đã có badge 7_DAYS lỡ chưa gắn) lên 1 lần cập nhật đạt mốc 30 -
        // đúng ra streak chỉ +1 mỗi lần gọi nên đây là kiểm tra badge 7_DAYS đã có từ trước không
        // bị thêm trùng, chỉ 30_DAYS được thêm mới.
        ReadingStreak existing = ReadingStreak.builder()
                .userId(USER_ID)
                .currentStreak(29)
                .longestStreak(29)
                .lastActiveDate(LocalDate.now(ZoneOffset.UTC).minusDays(1))
                .badges(new java.util.ArrayList<>(java.util.List.of("7_DAYS")))
                .build();
        when(readingStreakRepository.findById(USER_ID)).thenReturn(Optional.of(existing));

        readingProgressService.updateProgress(BOOK_ID, request(ReadingProgressStatus.READING));

        ArgumentCaptor<ReadingStreak> captor = ArgumentCaptor.forClass(ReadingStreak.class);
        org.mockito.Mockito.verify(readingStreakRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentStreak()).isEqualTo(30);
        assertThat(captor.getValue().getBadges()).containsExactlyInAnyOrder("7_DAYS", "30_DAYS");
    }

    @Test
    void getMyStreak_noRecordYet_returnsZeroedResponse() {
        when(readingStreakRepository.findById(USER_ID)).thenReturn(Optional.empty());

        ReadingStreakResponse response = readingProgressService.getMyStreak();

        assertThat(response.getCurrentStreak()).isZero();
        assertThat(response.getLongestStreak()).isZero();
        assertThat(response.getBadges()).isEmpty();
    }

    @Test
    void updateProgress_statusUnchanged_doesNotPublishSyncEvent() {
        ReadingProgress existingProgress = ReadingProgress.builder()
                .userId(USER_ID)
                .bookId(BOOK_ID)
                .status(ReadingProgressStatus.READING)
                .lastReadAt(Instant.now())
                .build();
        when(readingProgressRepository.findByUserIdAndBookId(USER_ID, BOOK_ID))
                .thenReturn(Optional.of(existingProgress));
        when(readingStreakRepository.findById(USER_ID)).thenReturn(Optional.empty());

        readingProgressService.updateProgress(BOOK_ID, request(ReadingProgressStatus.READING));

        org.mockito.Mockito.verify(outboxEventService, org.mockito.Mockito.never())
                .recordEvent(any(), any(), any());
    }

    @Test
    void updateProgress_statusChangedToCompleted_publishesReadingCompletedEvent() {
        ReadingProgress existingProgress = ReadingProgress.builder()
                .userId(USER_ID)
                .bookId(BOOK_ID)
                .status(ReadingProgressStatus.READING)
                .lastReadAt(Instant.now())
                .build();
        when(readingProgressRepository.findByUserIdAndBookId(USER_ID, BOOK_ID))
                .thenReturn(Optional.of(existingProgress));
        when(readingStreakRepository.findById(USER_ID)).thenReturn(Optional.empty());

        readingProgressService.updateProgress(BOOK_ID, request(ReadingProgressStatus.COMPLETED));

        org.mockito.Mockito.verify(outboxEventService)
                .recordEvent(
                        org.mockito.ArgumentMatchers.eq(BOOK_ID),
                        org.mockito.ArgumentMatchers.eq(com.devteria.reading.dto.OutboxEventType.READING_COMPLETED),
                        any());
    }
}
