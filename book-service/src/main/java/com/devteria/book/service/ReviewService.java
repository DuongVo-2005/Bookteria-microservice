package com.devteria.book.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.devteria.book.dto.OutboxEventType;
import com.devteria.book.dto.request.ReviewCreateRequest;
import com.devteria.book.dto.request.ReviewUpdateRequest;
import com.devteria.book.dto.response.ActiveReaderResponse;
import com.devteria.book.dto.response.PageResponse;
import com.devteria.book.dto.response.ReviewResponse;
import com.devteria.book.entity.BookStats;
import com.devteria.book.entity.Review;
import com.devteria.book.exception.AppException;
import com.devteria.book.exception.ErrorCode;
import com.devteria.book.mapper.BookMapper;
import com.devteria.book.mapper.ReviewMapper;
import com.devteria.book.repository.BookRepository;
import com.devteria.book.repository.ReviewRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewService {
    ReviewRepository reviewRepository;

    ReviewMapper reviewMapper;

    BookRepository bookRepository;

    BookService bookService;

    OutboxEventService outboxEventService;
    private final BookMapper bookMapper;

    public ReviewResponse createReview(String bookId, ReviewCreateRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        var book = bookRepository.findById(bookId).orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));
        // idea-spec BA v2 §2.1/P1-02: Review Bombing - LIBRARIAN/ADMIN khoá viết review mới cho
        // sách này, review cũ không bị ảnh hưởng.
        if (book.isReviewsLocked()) {
            throw new AppException(ErrorCode.REVIEWS_LOCKED_FOR_BOOK);
        }
        if (reviewRepository.existsByBookIdAndUserId(bookId, userId)) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }
        Review review = Review.builder()
                .bookId(bookId)
                .userId(userId)
                .rating(request.getRating())
                .content(request.getContent())
                .hasSpoiler(request.isHasSpoiler())
                .createdAt(Instant.now())
                .build();
        reviewRepository.save(review);

        BookStats bookStats = bookService.updateBookStatsAfterReview(bookId, request.getRating());
        Map<String, Object> payload = Map.of(
                "bookId", bookId,
                "reviewId", review.getId(),
                "rating", review.getRating(),
                "ratingAverage", bookStats.getRatingAverage(),
                "ratingCount", bookStats.getRatingCount());
        outboxEventService.recordEvent(bookId, OutboxEventType.BOOK_REVIEWED, payload);
        return reviewMapper.toReviewResponse(review);
    }

    public ReviewResponse updateReview(String reviewId, ReviewUpdateRequest request) {
        String currenUsrId =
                SecurityContextHolder.getContext().getAuthentication().getName();
        Review review =
                reviewRepository.findById(reviewId).orElseThrow(() -> new AppException((ErrorCode.REVIEW_NOT_FOUND)));

        if (!review.getUserId().equals(currenUsrId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        int oldRating = review.getRating();

        int newRating = request.getRating();

        review.setRating(newRating);
        review.setContent(request.getContent());
        review.setHasSpoiler(request.isHasSpoiler());
        review.setUpdatedAt(Instant.now());

        reviewRepository.save(review);

        bookService.updateBookStatsAfterReviewUpdate(review.getBookId(), oldRating, newRating);
        return reviewMapper.toReviewResponse(review);
    }

    public void deleteReview(String reviewId) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();
        Review review =
                reviewRepository.findById(reviewId).orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));
        // idea-spec Phase 4 - "14. Review Moderation": platform ADMIN xoá được review vi phạm
        // của người khác, không chỉ tác giả.
        if (!review.getUserId().equals(currentUserId) && !isAdmin()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        // idea-spec Phase 7 - "26. Security Hardening / Permission auditing": log có cấu trúc
        // cho hành động ADMIN xoá nội dung không phải của mình - Loki/Grafana đã sẵn (Phase 7
        // Centralized Logging), filter theo "[AUDIT]" là đủ để tra cứu, không cần entity DB mới.
        if (!review.getUserId().equals(currentUserId)) {
            log.info(
                    "[AUDIT] admin={} action=DELETE_REVIEW target=review:{} owner={}",
                    currentUserId,
                    reviewId,
                    review.getUserId());
        }
        String bookId = review.getBookId();
        int rating = review.getRating();

        reviewRepository.delete(review);

        bookService.updateBookStatsAfterReviewDelete(bookId, rating);
    }

    // idea-spec BA GAP-03: thực thi lệnh REMOVE_REVIEW từ ADMIN_COMMAND_EXECUTE (report-service)
    // — gọi từ Kafka consumer, không có SecurityContext nên không tái dùng deleteReview().
    public void removeReviewBySystem(String reviewId) {
        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            return;
        }
        String bookId = review.getBookId();
        int rating = review.getRating();
        reviewRepository.delete(review);
        bookService.updateBookStatsAfterReviewDelete(bookId, rating);
    }

    // idea-spec BA FEAT-03: Helpful Upvote - toggle (bấm lại để bỏ vote), lưu userId để chặn vote
    // nhiều lần. helpfulCount denormalize để getBooks() sort trực tiếp được bằng Spring Data Sort.
    public ReviewResponse toggleHelpful(String reviewId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Review review =
                reviewRepository.findById(reviewId).orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        boolean nowHelpful;
        if (review.getHelpfulUserIds().remove(userId)) {
            review.setHelpfulCount(Math.max(0, review.getHelpfulCount() - 1));
            nowHelpful = false;
        } else {
            review.getHelpfulUserIds().add(userId);
            review.setHelpfulCount(review.getHelpfulCount() + 1);
            nowHelpful = true;
        }
        reviewRepository.save(review);

        ReviewResponse response = reviewMapper.toReviewResponse(review);
        response.setHelpfulByMe(nowHelpful);
        return response;
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    // idea-spec BA FEAT-01 - Bước 3: "độc giả tích cực" đo bằng số review đã viết (tín hiệu tương
    // tác có sẵn, không cần thêm bảng theo dõi hoạt động riêng). Gộp trong Java trên toàn bộ
    // reviews (không Mongo aggregation pipeline) - cùng lý do quy mô nhỏ đã áp dụng ở BookService's
    // trending/RecommendationService.
    public List<ActiveReaderResponse> getTopActiveReaders(int limit) {
        Map<String, Long> reviewCountByUser = reviewRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Review::getUserId, java.util.stream.Collectors.counting()));

        return reviewCountByUser.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> ActiveReaderResponse.builder()
                        .userId(entry.getKey())
                        .reviewCount(entry.getValue())
                        .build())
                .toList();
    }

    public PageResponse<ReviewResponse> getBooks(int page, int size, String bookId) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();
        // idea-spec BA FEAT-03: "Review có lượng Upvote cao được ưu tiên xếp lên đầu" - đổi sort
        // mặc định từ createdAt-only sang helpfulCount trước, createdAt làm tiebreaker.
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "helpfulCount").and(Sort.by(Sort.Direction.DESC, "createdAt")));
        var pageData = reviewRepository.findAllByBookId(bookId, pageable);
        var bookList = pageData.stream()
                .map(review -> {
                    ReviewResponse response = reviewMapper.toReviewResponse(review);
                    response.setHelpfulByMe(review.getHelpfulUserIds().contains(currentUserId));
                    return response;
                })
                .toList();
        return PageResponse.<ReviewResponse>builder()
                .data(bookList)
                .currentPage(page)
                .pageSize(size)
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .build();
    }
}
