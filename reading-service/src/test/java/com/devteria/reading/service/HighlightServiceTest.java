package com.devteria.reading.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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

import com.devteria.reading.dto.request.HighlightCreateRequest;
import com.devteria.reading.dto.request.PostCreateRequest;
import com.devteria.reading.dto.request.ShareHighlightRequest;
import com.devteria.reading.dto.response.ApiResponse;
import com.devteria.reading.dto.response.BookLookupResponse;
import com.devteria.reading.dto.response.HighlightResponse;
import com.devteria.reading.dto.response.PostSummaryResponse;
import com.devteria.reading.dto.response.QuoteCardResponse;
import com.devteria.reading.entity.Highlight;
import com.devteria.reading.exception.AppException;
import com.devteria.reading.exception.ErrorCode;
import com.devteria.reading.mapper.HighlightMapper;
import com.devteria.reading.repository.HighlightRepository;
import com.devteria.reading.repository.httpclient.BookClient;
import com.devteria.reading.repository.httpclient.PostClient;

// Unit test thuần Mockito cho HighlightService - tập trung vào getQuoteCardData()/shareToPost()
// với mediaUrls (FEAT-04, code mới). Service này trước đây 0 test.
@ExtendWith(MockitoExtension.class)
class HighlightServiceTest {

    private static final String USER_ID = "user-1";
    private static final String HIGHLIGHT_ID = "highlight-1";
    private static final String BOOK_ID = "book-1";

    @Mock
    private HighlightRepository highlightRepository;

    @Mock
    private HighlightMapper highlightMapper;

    @Mock
    private PostClient postClient;

    @Mock
    private BookClient bookClient;

    private HighlightService highlightService;

    @BeforeEach
    void setUp() {
        highlightService = new HighlightService(highlightRepository, highlightMapper, postClient, bookClient);

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

    private Highlight highlight() {
        return Highlight.builder()
                .id(HIGHLIGHT_ID)
                .userId(USER_ID)
                .bookId(BOOK_ID)
                .selectedText("Đời thay đổi khi ta thay đổi")
                .build();
    }

    @Test
    void getQuoteCardData_returnsTextAndBookMetadata() {
        when(highlightRepository.findByIdAndUserId(HIGHLIGHT_ID, USER_ID)).thenReturn(Optional.of(highlight()));
        BookLookupResponse book = BookLookupResponse.builder()
                .id(BOOK_ID)
                .title("Đắc Nhân Tâm")
                .authors(List.of(BookLookupResponse.AuthorInfo.builder()
                        .name("Dale Carnegie")
                        .build()))
                .metadata(BookLookupResponse.Metadata.builder()
                        .coverImage("http://cover.jpg")
                        .build())
                .build();
        when(bookClient.getBookById(BOOK_ID))
                .thenReturn(
                        ApiResponse.<BookLookupResponse>builder().result(book).build());

        QuoteCardResponse response = highlightService.getQuoteCardData(HIGHLIGHT_ID);

        assertThat(response.getText()).isEqualTo("Đời thay đổi khi ta thay đổi");
        assertThat(response.getBookTitle()).isEqualTo("Đắc Nhân Tâm");
        assertThat(response.getAuthorName()).isEqualTo("Dale Carnegie");
        assertThat(response.getBookCoverImage()).isEqualTo("http://cover.jpg");
    }

    @Test
    void getQuoteCardData_highlightNotFound_throws() {
        when(highlightRepository.findByIdAndUserId(HIGHLIGHT_ID, USER_ID)).thenReturn(Optional.empty());

        var exception = assertThrows(AppException.class, () -> highlightService.getQuoteCardData(HIGHLIGHT_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.HIGHLIGHT_NOT_FOUND);
    }

    @Test
    void getQuoteCardData_bookNotFound_throws() {
        when(highlightRepository.findByIdAndUserId(HIGHLIGHT_ID, USER_ID)).thenReturn(Optional.of(highlight()));
        when(bookClient.getBookById(BOOK_ID))
                .thenReturn(ApiResponse.<BookLookupResponse>builder().build());

        var exception = assertThrows(AppException.class, () -> highlightService.getQuoteCardData(HIGHLIGHT_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    void shareToPost_withMediaUrls_forwardsToPostClient() {
        when(highlightRepository.findByIdAndUserId(HIGHLIGHT_ID, USER_ID)).thenReturn(Optional.of(highlight()));
        when(postClient.createPost(any()))
                .thenReturn(ApiResponse.<PostSummaryResponse>builder()
                        .result(PostSummaryResponse.builder().id("post-1").build())
                        .build());

        var request = ShareHighlightRequest.builder()
                .mediaUrls(List.of("http://file-service/media/download/quote-card.png"))
                .build();
        highlightService.shareToPost(HIGHLIGHT_ID, request);

        ArgumentCaptor<PostCreateRequest> captor = ArgumentCaptor.forClass(PostCreateRequest.class);
        verify(postClient).createPost(captor.capture());
        assertThat(captor.getValue().getMediaUrls())
                .containsExactly("http://file-service/media/download/quote-card.png");
    }

    @Test
    void shareToPost_nullRequest_stillWorksTextOnly() {
        when(highlightRepository.findByIdAndUserId(HIGHLIGHT_ID, USER_ID)).thenReturn(Optional.of(highlight()));
        when(postClient.createPost(any()))
                .thenReturn(ApiResponse.<PostSummaryResponse>builder()
                        .result(PostSummaryResponse.builder().id("post-1").build())
                        .build());

        highlightService.shareToPost(HIGHLIGHT_ID, null);

        ArgumentCaptor<PostCreateRequest> captor = ArgumentCaptor.forClass(PostCreateRequest.class);
        verify(postClient).createPost(captor.capture());
        assertThat(captor.getValue().getMediaUrls()).isNull();
    }

    // ---- createHighlight() dedupe theo clientId (BA v2 §3.1) ----

    @Test
    void createHighlight_retryWithSameClientId_returnsExistingWithoutCreatingDuplicate() {
        Highlight existing = Highlight.builder()
                .id(HIGHLIGHT_ID)
                .userId(USER_ID)
                .bookId(BOOK_ID)
                .selectedText("Đời thay đổi khi ta thay đổi")
                .clientId("client-key-1")
                .build();
        when(highlightRepository.findByUserIdAndClientId(USER_ID, "client-key-1"))
                .thenReturn(Optional.of(existing));
        when(highlightMapper.toHighlightResponse(existing))
                .thenReturn(HighlightResponse.builder().id(HIGHLIGHT_ID).build());

        HighlightResponse response = highlightService.createHighlight(HighlightCreateRequest.builder()
                .bookId(BOOK_ID)
                .chapterId("chapter-1")
                .selectedText("text")
                .color("yellow")
                .clientId("client-key-1")
                .build());

        assertThat(response.getId()).isEqualTo(HIGHLIGHT_ID);
        verify(highlightRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void createHighlight_noClientId_alwaysCreatesNew() {
        when(highlightRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(highlightMapper.toHighlightResponse(any()))
                .thenReturn(HighlightResponse.builder().build());

        highlightService.createHighlight(HighlightCreateRequest.builder()
                .bookId(BOOK_ID)
                .chapterId("chapter-1")
                .selectedText("text")
                .color("yellow")
                .build());

        verify(highlightRepository, org.mockito.Mockito.never()).findByUserIdAndClientId(any(), any());
        verify(highlightRepository).save(any());
    }
}
