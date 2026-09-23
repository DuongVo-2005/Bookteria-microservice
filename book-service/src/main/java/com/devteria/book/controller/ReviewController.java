package com.devteria.book.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.devteria.book.dto.request.ReviewCreateRequest;
import com.devteria.book.dto.request.ReviewUpdateRequest;
import com.devteria.book.dto.response.ActiveReaderResponse;
import com.devteria.book.dto.response.ApiResponse;
import com.devteria.book.dto.response.PageResponse;
import com.devteria.book.dto.response.ReviewResponse;
import com.devteria.book.service.ReviewService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewController {
    ReviewService reviewService;

    // idea-spec BA FEAT-01 - Bước 3
    @GetMapping("/reviews/active-readers")
    public ApiResponse<List<ActiveReaderResponse>> getActiveReaders(
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        return ApiResponse.<List<ActiveReaderResponse>>builder()
                .result(reviewService.getTopActiveReaders(limit))
                .build();
    }

    @PostMapping("/books/{bookId}/reviews")
    public ApiResponse<ReviewResponse> createReview(
            @PathVariable("bookId") String bookId, @RequestBody @Valid ReviewCreateRequest request) {
        return ApiResponse.<ReviewResponse>builder()
                .result(reviewService.createReview(bookId, request))
                .build();
    }

    @GetMapping("/books/{bookId}/reviews")
    public ApiResponse<PageResponse<ReviewResponse>> getReviewsByBookId(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @PathVariable(value = "bookId") String bookId) {

        return ApiResponse.<PageResponse<ReviewResponse>>builder()
                .result(reviewService.getBooks(page, size, bookId))
                .build();
    }

    @PutMapping("/reviews/{reviewId}")
    public ApiResponse<ReviewResponse> updateReview(
            @PathVariable(value = "reviewId") String reviewId, @RequestBody @Valid ReviewUpdateRequest request) {

        return ApiResponse.<ReviewResponse>builder()
                .result(reviewService.updateReview(reviewId, request))
                .build();
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ApiResponse<String> deleteReview(@PathVariable(value = "reviewId") String reviewId) {
        reviewService.deleteReview(reviewId);
        return ApiResponse.<String>builder().result("Delete success").build();
    }

    // idea-spec BA FEAT-03: bấm lại để bỏ vote (toggle), không phải endpoint riêng cho vote/unvote.
    @PostMapping("/reviews/{reviewId}/helpful")
    public ApiResponse<ReviewResponse> toggleHelpful(@PathVariable(value = "reviewId") String reviewId) {
        return ApiResponse.<ReviewResponse>builder()
                .result(reviewService.toggleHelpful(reviewId))
                .build();
    }
}
