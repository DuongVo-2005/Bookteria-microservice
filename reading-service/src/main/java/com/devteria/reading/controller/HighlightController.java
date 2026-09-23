package com.devteria.reading.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.devteria.reading.dto.request.HighlightCreateRequest;
import com.devteria.reading.dto.request.HighlightUpdateRequest;
import com.devteria.reading.dto.request.ShareHighlightRequest;
import com.devteria.reading.dto.response.ApiResponse;
import com.devteria.reading.dto.response.HighlightResponse;
import com.devteria.reading.dto.response.PostSummaryResponse;
import com.devteria.reading.dto.response.QuoteCardResponse;
import com.devteria.reading.service.HighlightService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HighlightController {
    HighlightService highlightService;

    @GetMapping("/books/{bookId}/highlights")
    ApiResponse<List<HighlightResponse>> getHighlights(@PathVariable String bookId) {
        return ApiResponse.<List<HighlightResponse>>builder()
                .result(highlightService.getHighlights(bookId))
                .build();
    }

    @PostMapping("/highlights")
    ApiResponse<HighlightResponse> createHighlight(@RequestBody @Valid HighlightCreateRequest request) {
        return ApiResponse.<HighlightResponse>builder()
                .result(highlightService.createHighlight(request))
                .build();
    }

    @PatchMapping("/highlights/{highlightId}")
    ApiResponse<HighlightResponse> updateHighlight(
            @PathVariable String highlightId, @RequestBody @Valid HighlightUpdateRequest request) {
        return ApiResponse.<HighlightResponse>builder()
                .result(highlightService.updateHighlight(highlightId, request))
                .build();
    }

    @DeleteMapping("/highlights/{highlightId}")
    ApiResponse<Void> deleteHighlight(@PathVariable String highlightId) {
        highlightService.deleteHighlight(highlightId);
        return ApiResponse.<Void>builder().message("Highlight deleted").build();
    }

    // idea-spec Phase 5 - "18.1 Book Quotes/Highlights Sharing" / BA FEAT-04 (mediaUrls optional)
    @PostMapping("/highlights/{highlightId}/share")
    ApiResponse<PostSummaryResponse> shareHighlight(
            @PathVariable String highlightId, @RequestBody(required = false) ShareHighlightRequest request) {
        return ApiResponse.<PostSummaryResponse>builder()
                .result(highlightService.shareToPost(highlightId, request))
                .build();
    }

    // idea-spec BA FEAT-04: Quote Card Generator - dữ liệu để FE tự vẽ thiệp trước khi share.
    @GetMapping("/highlights/{highlightId}/quote-card")
    ApiResponse<QuoteCardResponse> getQuoteCardData(@PathVariable String highlightId) {
        return ApiResponse.<QuoteCardResponse>builder()
                .result(highlightService.getQuoteCardData(highlightId))
                .build();
    }
}
