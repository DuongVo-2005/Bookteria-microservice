package com.devteria.reading.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.devteria.reading.dto.request.ReadingProgressUpdateRequest;
import com.devteria.reading.dto.response.ApiResponse;
import com.devteria.reading.dto.response.ReadingProgressResponse;
import com.devteria.reading.dto.response.ReadingStreakResponse;
import com.devteria.reading.service.ReadingProgressService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/progress")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReadingProgressController {
    ReadingProgressService readingProgressService;

    @GetMapping("/{bookId}")
    ApiResponse<ReadingProgressResponse> getProgress(@PathVariable String bookId) {
        return ApiResponse.<ReadingProgressResponse>builder()
                .result(readingProgressService.getProgress(bookId))
                .build();
    }

    @GetMapping
    ApiResponse<List<ReadingProgressResponse>> getAllProgress() {
        return ApiResponse.<List<ReadingProgressResponse>>builder()
                .result(readingProgressService.getAllProgress())
                .build();
    }

    // idea-spec BA FEAT-02
    @GetMapping("/me/streak")
    ApiResponse<ReadingStreakResponse> getMyStreak() {
        return ApiResponse.<ReadingStreakResponse>builder()
                .result(readingProgressService.getMyStreak())
                .build();
    }

    @PutMapping("/{bookId}")
    ApiResponse<ReadingProgressResponse> updateProgress(
            @PathVariable String bookId, @RequestBody @Valid ReadingProgressUpdateRequest request) {
        return ApiResponse.<ReadingProgressResponse>builder()
                .result(readingProgressService.updateProgress(bookId, request))
                .build();
    }
}
