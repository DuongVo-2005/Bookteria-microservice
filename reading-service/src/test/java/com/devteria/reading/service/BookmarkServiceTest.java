package com.devteria.reading.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.devteria.reading.dto.request.BookmarkCreateRequest;
import com.devteria.reading.dto.response.BookmarkResponse;
import com.devteria.reading.entity.Bookmark;
import com.devteria.reading.mapper.BookmarkMapper;
import com.devteria.reading.repository.BookmarkRepository;

// Unit test thuần Mockito cho BookmarkService - service này trước đây 0 test. Trọng tâm: dedupe
// theo clientId (BA v2 §3.1 Offline Sync Batch) khi client gửi lại đúng 1 batch 2 lần.
@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    private static final String USER_ID = "user-1";
    private static final String BOOK_ID = "book-1";
    private static final String BOOKMARK_ID = "bookmark-1";

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private BookmarkMapper bookmarkMapper;

    private BookmarkService bookmarkService;

    @BeforeEach
    void setUp() {
        bookmarkService = new BookmarkService(bookmarkRepository, bookmarkMapper);

        SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(USER_ID);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createBookmark_retryWithSameClientId_returnsExistingWithoutCreatingDuplicate() {
        Bookmark existing = Bookmark.builder()
                .id(BOOKMARK_ID)
                .userId(USER_ID)
                .bookId(BOOK_ID)
                .clientId("client-key-1")
                .build();
        when(bookmarkRepository.findByUserIdAndClientId(USER_ID, "client-key-1"))
                .thenReturn(Optional.of(existing));
        when(bookmarkMapper.toBookmarkResponse(existing))
                .thenReturn(BookmarkResponse.builder().id(BOOKMARK_ID).build());

        BookmarkResponse response = bookmarkService.createBookmark(BookmarkCreateRequest.builder()
                .bookId(BOOK_ID)
                .chapterId("chapter-1")
                .clientId("client-key-1")
                .build());

        assertThat(response.getId()).isEqualTo(BOOKMARK_ID);
        verify(bookmarkRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void createBookmark_noClientId_alwaysCreatesNew() {
        when(bookmarkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookmarkMapper.toBookmarkResponse(any()))
                .thenReturn(BookmarkResponse.builder().build());

        bookmarkService.createBookmark(BookmarkCreateRequest.builder()
                .bookId(BOOK_ID)
                .chapterId("chapter-1")
                .build());

        verify(bookmarkRepository, org.mockito.Mockito.never()).findByUserIdAndClientId(any(), any());
        verify(bookmarkRepository).save(any());
    }
}
