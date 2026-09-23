package com.devteria.book.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.devteria.book.dto.ReadingStatus;
import com.devteria.book.dto.WrapUpPeriod;
import com.devteria.book.dto.request.ReadingChallengeUpsertRequest;
import com.devteria.book.dto.request.ReadingListCreateRequest;
import com.devteria.book.dto.request.ReadingListUpdateRequest;
import com.devteria.book.dto.request.ShelfPrivacyUpdateRequest;
import com.devteria.book.dto.response.ApiResponse;
import com.devteria.book.dto.response.PageResponse;
import com.devteria.book.dto.response.ReadingChallengeResponse;
import com.devteria.book.dto.response.ReadingListResponse;
import com.devteria.book.dto.response.ReadingStatsResponse;
import com.devteria.book.dto.response.ReadingWrapUpResponse;
import com.devteria.book.dto.response.ShelfPrivacyResponse;
import com.devteria.book.service.ReadingListService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReadingListController {
    ReadingListService readingListService;

    @PostMapping("/me/reading-list")
    public ApiResponse<ReadingListResponse> addToShelf(@RequestBody @Valid ReadingListCreateRequest request) {
        return ApiResponse.<ReadingListResponse>builder()
                .result(readingListService.addToShelf(request.getBookId(), request))
                .build();
    }

    // idea-spec Phase 5 - "17. Reading Statistics"
    @GetMapping("/me/reading-list/stats")
    public ApiResponse<ReadingStatsResponse> getMyStats() {
        return ApiResponse.<ReadingStatsResponse>builder()
                .result(readingListService.getMyStats())
                .build();
    }

    // idea-spec BA v2 §4.2: Reading Wrap-up
    @GetMapping("/me/reading-list/wrapup")
    public ApiResponse<ReadingWrapUpResponse> getMyWrapUp(
            @RequestParam(value = "period", defaultValue = "MONTH") WrapUpPeriod period) {
        return ApiResponse.<ReadingWrapUpResponse>builder()
                .result(readingListService.getMyWrapUp(period))
                .build();
    }

    @GetMapping("/me/reading-list")
    public ApiResponse<PageResponse<ReadingListResponse>> getMyReadingList(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) ReadingStatus status) {

        return ApiResponse.<PageResponse<ReadingListResponse>>builder()
                .result(readingListService.getMyReadingList(page, size, status))
                .build();
    }

    // idea-spec BA v2 §4.3: Shelf Privacy
    @GetMapping("/me/shelf-privacy")
    public ApiResponse<ShelfPrivacyResponse> getMyShelfPrivacy() {
        return ApiResponse.<ShelfPrivacyResponse>builder()
                .result(readingListService.getMyShelfPrivacy())
                .build();
    }

    @PatchMapping("/me/shelf-privacy")
    public ApiResponse<ShelfPrivacyResponse> updateMyShelfPrivacy(
            @RequestBody @Valid ShelfPrivacyUpdateRequest request) {
        return ApiResponse.<ShelfPrivacyResponse>builder()
                .result(readingListService.updateMyShelfPrivacy(request))
                .build();
    }

    // idea-spec BA v2 §4.3: xem Kệ sách của người khác, tôn trọng cài đặt riêng tư của họ.
    @GetMapping("/users/{userId}/reading-list")
    public ApiResponse<PageResponse<ReadingListResponse>> getUserReadingList(
            @PathVariable(value = "userId") String userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) ReadingStatus status) {

        return ApiResponse.<PageResponse<ReadingListResponse>>builder()
                .result(readingListService.getUserReadingList(userId, page, size, status))
                .build();
    }

    @PatchMapping("/me/reading-list/{id}")
    public ApiResponse<ReadingListResponse> updateProgress(
            @PathVariable(value = "id") String id, @RequestBody @Valid ReadingListUpdateRequest request) {

        return ApiResponse.<ReadingListResponse>builder()
                .result(readingListService.updateProgress(id, request))
                .build();
    }

    @DeleteMapping("/me/reading-list/{id}")
    public ApiResponse<String> deleteReadingList(@PathVariable(value = "id") String id) {
        readingListService.removeFromShelf(id);
        return ApiResponse.<String>builder().result("Delete success").build();
    }

    // idea-spec BA FEAT-02
    @PostMapping("/me/reading-challenge")
    public ApiResponse<ReadingChallengeResponse> setMyReadingChallenge(
            @RequestBody @Valid ReadingChallengeUpsertRequest request) {
        return ApiResponse.<ReadingChallengeResponse>builder()
                .result(readingListService.setMyReadingChallenge(request))
                .build();
    }

    @GetMapping("/me/reading-challenge")
    public ApiResponse<ReadingChallengeResponse> getMyReadingChallenge(@RequestParam(value = "year") int year) {
        return ApiResponse.<ReadingChallengeResponse>builder()
                .result(readingListService.getMyReadingChallenge(year))
                .build();
    }

    @GetMapping("/books/{bookId}/reading-progress")
    public ApiResponse<ReadingListResponse> getMyProgressByBookId(@PathVariable(value = "bookId") String bookId) {
        return ApiResponse.<ReadingListResponse>builder()
                .result(readingListService.getMyProgressByBookId(bookId))
                .build();
    }

    @PutMapping("/books/{bookId}/reading-progress")
    public ApiResponse<ReadingListResponse> upsertMyProgressByBookId(
            @PathVariable(value = "bookId") String bookId, @RequestBody @Valid ReadingListUpdateRequest request) {
        return ApiResponse.<ReadingListResponse>builder()
                .result(readingListService.upsertMyProgressByBookId(bookId, request))
                .build();
    }
}
