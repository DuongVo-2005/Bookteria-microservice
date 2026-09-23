package com.devteria.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.devteria.book.dto.request.ReviewCreateRequest;
import com.devteria.book.dto.response.ReviewResponse;
import com.devteria.book.entity.BookStats;
import com.devteria.book.entity.Review;
import com.devteria.book.exception.AppException;
import com.devteria.book.exception.ErrorCode;
import com.devteria.book.mapper.BookMapper;
import com.devteria.book.mapper.ReviewMapper;
import com.devteria.book.repository.BookRepository;
import com.devteria.book.repository.ReviewRepository;

// Unit test thuần Mockito cho ReviewService - service này trước đây 0 test. Tập trung vào
// toggleHelpful()/hasSpoiler pass-through (FEAT-03, code mới).
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final String USER_ID = "user-1";
    private static final String BOOK_ID = "book-1";
    private static final String REVIEW_ID = "review-1";

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookService bookService;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private BookMapper bookMapper;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
                reviewRepository, reviewMapper, bookRepository, bookService, outboxEventService, bookMapper);

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

    private Review review(java.util.Set<String> helpfulUserIds, int helpfulCount) {
        return Review.builder()
                .id(REVIEW_ID)
                .bookId(BOOK_ID)
                .userId("author-1")
                .rating(5)
                .helpfulUserIds(helpfulUserIds)
                .helpfulCount(helpfulCount)
                .build();
    }

    @Test
    void toggleHelpful_notYetVoted_addsVoteAndIncrementsCount() {
        Review review = review(new HashSet<>(), 2);
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewMapper.toReviewResponse(review))
                .thenReturn(ReviewResponse.builder().build());

        ReviewResponse response = reviewService.toggleHelpful(REVIEW_ID);

        assertThat(review.getHelpfulUserIds()).contains(USER_ID);
        assertThat(review.getHelpfulCount()).isEqualTo(3);
        assertThat(response.isHelpfulByMe()).isTrue();
        verify(reviewRepository).save(review);
    }

    @Test
    void toggleHelpful_alreadyVoted_removesVoteAndDecrementsCount() {
        Review review = review(new HashSet<>(java.util.List.of(USER_ID)), 3);
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewMapper.toReviewResponse(review))
                .thenReturn(ReviewResponse.builder().build());

        ReviewResponse response = reviewService.toggleHelpful(REVIEW_ID);

        assertThat(review.getHelpfulUserIds()).doesNotContain(USER_ID);
        assertThat(review.getHelpfulCount()).isEqualTo(2);
        assertThat(response.isHelpfulByMe()).isFalse();
    }

    @Test
    void toggleHelpful_countNeverGoesNegative() {
        Review review = review(new HashSet<>(java.util.List.of(USER_ID)), 0);
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewMapper.toReviewResponse(review))
                .thenReturn(ReviewResponse.builder().build());

        reviewService.toggleHelpful(REVIEW_ID);

        assertThat(review.getHelpfulCount()).isZero();
    }

    @Test
    void toggleHelpful_reviewNotFound_throws() {
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        var exception = org.junit.jupiter.api.Assertions.assertThrows(
                AppException.class, () -> reviewService.toggleHelpful(REVIEW_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND);
    }

    @Test
    void createReview_passesHasSpoilerFromRequestToEntity() {
        when(bookRepository.findById(BOOK_ID))
                .thenReturn(Optional.of(com.devteria.book.entity.Book.builder()
                        .id(BOOK_ID)
                        .reviewsLocked(false)
                        .build()));
        when(reviewRepository.existsByBookIdAndUserId(BOOK_ID, USER_ID)).thenReturn(false);
        when(bookService.updateBookStatsAfterReview(any(), any(Integer.class)))
                .thenReturn(BookStats.builder()
                        .ratingAverage(java.math.BigDecimal.valueOf(4.5))
                        .ratingCount(2L)
                        .build());
        // reviewRepository.save() thật (Mongo) tự sinh _id ngược lên entity - mock không tự làm
        // điều đó, phải giả lập lại vì createReview() đọc review.getId() ngay sau save() để build
        // payload Outbox.
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            Review saved = inv.getArgument(0);
            saved.setId(REVIEW_ID);
            return saved;
        });
        when(reviewMapper.toReviewResponse(any()))
                .thenReturn(ReviewResponse.builder().build());

        var request = ReviewCreateRequest.builder()
                .rating(4)
                .content("Spoiler: nhân vật chính chết ở cuối")
                .hasSpoiler(true)
                .build();
        reviewService.createReview(BOOK_ID, request);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().isHasSpoiler()).isTrue();
    }

    @Test
    void createReview_bookHasReviewsLocked_throws() {
        when(bookRepository.findById(BOOK_ID))
                .thenReturn(Optional.of(com.devteria.book.entity.Book.builder()
                        .id(BOOK_ID)
                        .reviewsLocked(true)
                        .build()));

        var exception = org.junit.jupiter.api.Assertions.assertThrows(
                AppException.class,
                () -> reviewService.createReview(
                        BOOK_ID, ReviewCreateRequest.builder().rating(5).build()));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REVIEWS_LOCKED_FOR_BOOK);
        verify(reviewRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void createReview_bookNotFound_throws() {
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.empty());

        var exception = org.junit.jupiter.api.Assertions.assertThrows(
                AppException.class,
                () -> reviewService.createReview(
                        BOOK_ID, ReviewCreateRequest.builder().rating(5).build()));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    void getBooks_sortsHelpfulCountDescendingThenCreatedAtDescending() {
        when(reviewRepository.findAllByBookId(org.mockito.ArgumentMatchers.eq(BOOK_ID), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Page<Review> emptyPage = new PageImpl<>(java.util.List.of());
                    return emptyPage;
                });

        reviewService.getBooks(0, 10, BOOK_ID);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewRepository).findAllByBookId(org.mockito.ArgumentMatchers.eq(BOOK_ID), pageableCaptor.capture());
        var sort = pageableCaptor.getValue().getSort();
        assertThat(sort.getOrderFor("helpfulCount").isDescending()).isTrue();
        assertThat(sort.getOrderFor("createdAt").isDescending()).isTrue();
    }

    @Test
    void getBooks_populatesHelpfulByMeForCurrentUser() {
        Review reviewVotedByMe = review(new HashSet<>(java.util.List.of(USER_ID)), 1);
        when(reviewRepository.findAllByBookId(org.mockito.ArgumentMatchers.eq(BOOK_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(reviewVotedByMe)));
        when(reviewMapper.toReviewResponse(reviewVotedByMe))
                .thenReturn(ReviewResponse.builder().build());

        var page = reviewService.getBooks(0, 10, BOOK_ID);

        assertThat(page.getData().getFirst().isHelpfulByMe()).isTrue();
    }
}
