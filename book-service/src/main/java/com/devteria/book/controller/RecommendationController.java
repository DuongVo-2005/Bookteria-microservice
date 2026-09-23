package com.devteria.book.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devteria.book.dto.response.ApiResponse;
import com.devteria.book.dto.response.BookResponse;
import com.devteria.book.service.RecommendationService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

// idea-spec Phase 6 - "21. Book Recommendation"
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RecommendationController {
    RecommendationService recommendationService;

    @GetMapping("/me/recommendations")
    public ApiResponse<List<BookResponse>> getMyRecommendations() {
        return ApiResponse.<List<BookResponse>>builder()
                .result(recommendationService.getMyRecommendations())
                .build();
    }
}
