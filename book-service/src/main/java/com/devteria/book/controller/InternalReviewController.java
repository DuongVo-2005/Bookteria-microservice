package com.devteria.book.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.devteria.book.dto.response.ApiResponse;
import com.devteria.book.repository.ReviewRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

// idea-spec BA GAP-03: report-service "validate nhẹ" targetId trước khi lưu Report PENDING.
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalReviewController {
    ReviewRepository reviewRepository;

    @GetMapping("/internal/reviews/{reviewId}/exists")
    ApiResponse<Boolean> reviewExists(@PathVariable String reviewId) {
        return ApiResponse.<Boolean>builder()
                .result(reviewRepository.existsById(reviewId))
                .build();
    }
}
