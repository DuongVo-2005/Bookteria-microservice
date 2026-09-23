package com.devteria.book.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.devteria.book.dto.request.GoogleBatchImportRequest;
import com.devteria.book.dto.response.ApiResponse;
import com.devteria.book.dto.response.BatchImportResponse;
import com.devteria.book.dto.response.BookResponse;
import com.devteria.book.dto.response.GoogleBookSearchItemResponse;
import com.devteria.book.dto.response.PageResponse;
import com.devteria.book.service.GoogleBooksImportService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GoogleBooksController {
    GoogleBooksImportService googleBooksImportService;

    @GetMapping("/books/google/search")
    public ApiResponse<PageResponse<GoogleBookSearchItemResponse>> search(
            @RequestParam(value = "q") String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        var result = googleBooksImportService.searchGoogleBooks(q, page, size);
        return ApiResponse.<PageResponse<GoogleBookSearchItemResponse>>builder()
                .result(result)
                .build();
    }

    // idea-spec BA v2 P1-02: book:import cho LIBRARIAN.
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('book:import')")
    @PostMapping("/books/google/import/{googleBookId}")
    public ApiResponse<BookResponse> importOne(@PathVariable(value = "googleBookId") String googleBookId) {
        var book = googleBooksImportService.importByGoogleBookId(googleBookId);
        return ApiResponse.<BookResponse>builder().result(book).build();
    }

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('book:import')")
    @PostMapping("/books/google/import")
    public ApiResponse<BatchImportResponse> batchImport(@RequestBody @Valid GoogleBatchImportRequest request) {
        var result = googleBooksImportService.batchImportFromGoogle(
                request.getQuery(), request.getMaxResults(), request.getStartIndex());
        return ApiResponse.<BatchImportResponse>builder().result(result).build();
    }
}
