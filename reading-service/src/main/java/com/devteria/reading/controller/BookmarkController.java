package com.devteria.reading.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.devteria.reading.dto.request.BookmarkCreateRequest;
import com.devteria.reading.dto.response.ApiResponse;
import com.devteria.reading.dto.response.BookmarkResponse;
import com.devteria.reading.service.BookmarkService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookmarkController {
    BookmarkService bookmarkService;

    @GetMapping("/books/{bookId}/bookmarks")
    ApiResponse<List<BookmarkResponse>> getBookmarks(@PathVariable String bookId) {
        return ApiResponse.<List<BookmarkResponse>>builder()
                .result(bookmarkService.getBookmarks(bookId))
                .build();
    }

    @PostMapping("/bookmarks")
    ApiResponse<BookmarkResponse> createBookmark(@RequestBody @Valid BookmarkCreateRequest request) {
        return ApiResponse.<BookmarkResponse>builder()
                .result(bookmarkService.createBookmark(request))
                .build();
    }

    @DeleteMapping("/bookmarks/{bookmarkId}")
    ApiResponse<Void> deleteBookmark(@PathVariable String bookmarkId) {
        bookmarkService.deleteBookmark(bookmarkId);
        return ApiResponse.<Void>builder().message("Bookmark deleted").build();
    }
}
