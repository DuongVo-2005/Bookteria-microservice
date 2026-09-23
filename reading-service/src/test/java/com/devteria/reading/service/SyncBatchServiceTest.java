package com.devteria.reading.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.devteria.reading.dto.ReadingProgressStatus;
import com.devteria.reading.dto.request.BookmarkCreateRequest;
import com.devteria.reading.dto.request.HighlightCreateRequest;
import com.devteria.reading.dto.request.ProgressSyncItem;
import com.devteria.reading.dto.request.SyncBatchRequest;
import com.devteria.reading.dto.response.BookmarkResponse;
import com.devteria.reading.dto.response.HighlightResponse;
import com.devteria.reading.dto.response.ReadingProgressResponse;
import com.devteria.reading.dto.response.SyncBatchResponse;
import com.devteria.reading.entity.Bookmark;
import com.devteria.reading.entity.Highlight;
import com.devteria.reading.entity.ReadingProgress;
import com.devteria.reading.mapper.BookmarkMapper;
import com.devteria.reading.mapper.HighlightMapper;
import com.devteria.reading.mapper.ReadingProgressMapper;
import com.devteria.reading.repository.BookmarkRepository;
import com.devteria.reading.repository.HighlightRepository;
import com.devteria.reading.repository.ReadingProgressRepository;
import com.devteria.reading.repository.ReadingStreakRepository;

// idea-spec BA v2 §3.1 Offline Sync Batch - trọng tâm: 1 write cũ hơn (clientUpdatedAt trước
// lastReadAt đã lưu) trong batch bị BỎ QUA thay vì ghi đè (Progress Monotonicity), còn
// highlight/bookmark trong batch vẫn được tạo bình thường (luôn là CREATE, không có khái niệm
// "cũ hơn").
@ExtendWith(MockitoExtension.class)
class SyncBatchServiceTest {

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

    @Mock
    private HighlightRepository highlightRepository;

    @Mock
    private HighlightMapper highlightMapper;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private BookmarkMapper bookmarkMapper;

    private SyncBatchService syncBatchService;

    @BeforeEach
    void setUp() {
        ReadingProgressService readingProgressService = new ReadingProgressService(
                readingProgressRepository, readingProgressMapper, outboxEventService, readingStreakRepository);
        HighlightService highlightService = new HighlightService(highlightRepository, highlightMapper, null, null);
        BookmarkService bookmarkService = new BookmarkService(bookmarkRepository, bookmarkMapper);

        syncBatchService = new SyncBatchService(readingProgressService, highlightService, bookmarkService);

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(USER_ID);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        org.mockito.Mockito.lenient()
                .when(readingProgressMapper.toReadingProgressResponse(any()))
                .thenReturn(ReadingProgressResponse.builder().bookId(BOOK_ID).build());
        org.mockito.Mockito.lenient()
                .when(readingProgressRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.lenient()
                .when(readingStreakRepository.findById(USER_ID))
                .thenReturn(Optional.empty());
        org.mockito.Mockito.lenient()
                .when(highlightMapper.toHighlightResponse(any()))
                .thenReturn(HighlightResponse.builder().build());
        org.mockito.Mockito.lenient().when(highlightRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.lenient()
                .when(bookmarkMapper.toBookmarkResponse(any()))
                .thenReturn(BookmarkResponse.builder().build());
        org.mockito.Mockito.lenient().when(bookmarkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void applyBatch_progressItemOlderThanSaved_isSkippedNotOverwritten() {
        Instant now = Instant.now();
        ReadingProgress existing = ReadingProgress.builder()
                .userId(USER_ID)
                .bookId(BOOK_ID)
                .status(ReadingProgressStatus.READING)
                .progressPercent(80)
                .lastReadAt(now)
                .build();
        when(readingProgressRepository.findByUserIdAndBookId(USER_ID, BOOK_ID)).thenReturn(Optional.of(existing));

        ProgressSyncItem staleItem = ProgressSyncItem.builder()
                .bookId(BOOK_ID)
                .currentChapterId("chapter-1")
                .progressPercent(10)
                .status(ReadingProgressStatus.READING)
                .clientUpdatedAt(now.minus(1, ChronoUnit.HOURS))
                .build();
        SyncBatchRequest request =
                SyncBatchRequest.builder().progress(List.of(staleItem)).build();

        SyncBatchResponse response = syncBatchService.applyBatch(request);

        assertThat(response.getProgressApplied()).isZero();
        assertThat(response.getProgressSkipped()).isEqualTo(1);
        // progress trong DB không hề bị ghi đè xuống 10.
        assertThat(existing.getProgressPercent()).isEqualTo(80);
    }

    @Test
    void applyBatch_progressItemNewerThanSaved_isApplied() {
        Instant now = Instant.now();
        ReadingProgress existing = ReadingProgress.builder()
                .userId(USER_ID)
                .bookId(BOOK_ID)
                .status(ReadingProgressStatus.READING)
                .progressPercent(20)
                .lastReadAt(now.minus(1, ChronoUnit.HOURS))
                .build();
        when(readingProgressRepository.findByUserIdAndBookId(USER_ID, BOOK_ID)).thenReturn(Optional.of(existing));

        ProgressSyncItem freshItem = ProgressSyncItem.builder()
                .bookId(BOOK_ID)
                .currentChapterId("chapter-2")
                .progressPercent(90)
                .status(ReadingProgressStatus.READING)
                .clientUpdatedAt(now)
                .build();
        SyncBatchRequest request =
                SyncBatchRequest.builder().progress(List.of(freshItem)).build();

        SyncBatchResponse response = syncBatchService.applyBatch(request);

        assertThat(response.getProgressApplied()).isEqualTo(1);
        assertThat(response.getProgressSkipped()).isZero();
        assertThat(existing.getProgressPercent()).isEqualTo(90);
    }

    @Test
    void applyBatch_highlightsAndBookmarks_areCreatedWithClientTimestamp() {
        when(highlightRepository.save(any(Highlight.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookmarkRepository.save(any(Bookmark.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant clientCreatedAt = Instant.now().minus(2, ChronoUnit.DAYS);
        HighlightCreateRequest highlightItem = HighlightCreateRequest.builder()
                .bookId(BOOK_ID)
                .chapterId("chapter-1")
                .selectedText("quote")
                .color("yellow")
                .createdAt(clientCreatedAt)
                .build();
        BookmarkCreateRequest bookmarkItem = BookmarkCreateRequest.builder()
                .bookId(BOOK_ID)
                .chapterId("chapter-1")
                .createdAt(clientCreatedAt)
                .build();
        SyncBatchRequest request = SyncBatchRequest.builder()
                .highlights(List.of(highlightItem))
                .bookmarks(List.of(bookmarkItem))
                .build();

        SyncBatchResponse response = syncBatchService.applyBatch(request);

        assertThat(response.getHighlightsCreated()).hasSize(1);
        assertThat(response.getBookmarksCreated()).hasSize(1);

        org.mockito.ArgumentCaptor<Highlight> highlightCaptor = org.mockito.ArgumentCaptor.forClass(Highlight.class);
        org.mockito.Mockito.verify(highlightRepository).save(highlightCaptor.capture());
        assertThat(highlightCaptor.getValue().getCreatedAt()).isEqualTo(clientCreatedAt);

        org.mockito.ArgumentCaptor<Bookmark> bookmarkCaptor = org.mockito.ArgumentCaptor.forClass(Bookmark.class);
        org.mockito.Mockito.verify(bookmarkRepository).save(bookmarkCaptor.capture());
        assertThat(bookmarkCaptor.getValue().getCreatedAt()).isEqualTo(clientCreatedAt);
    }
}
