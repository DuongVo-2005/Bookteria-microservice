package com.devteria.reading.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.devteria.reading.dto.request.ChapterCreateRequest;
import com.devteria.reading.dto.response.ApiResponse;
import com.devteria.reading.dto.response.ChapterImportResponse;
import com.devteria.reading.dto.response.ChapterResponse;
import com.devteria.reading.dto.response.ChapterSummaryResponse;
import com.devteria.reading.dto.response.PageResponse;
import com.devteria.reading.service.BookContentImportService;
import com.devteria.reading.service.ChapterService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChapterController {
    ChapterService chapterService;
    BookContentImportService bookContentImportService;

    @GetMapping("/books/{bookId}/chapters")
    ApiResponse<PageResponse<ChapterSummaryResponse>> getChapters(
            @PathVariable String bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<ChapterSummaryResponse> result = chapterService.getChapters(bookId, page, size);
        return ApiResponse.<PageResponse<ChapterSummaryResponse>>builder()
                .result(PageResponse.<ChapterSummaryResponse>builder()
                        .currentPage(result.getNumber())
                        .totalPages(result.getTotalPages())
                        .pageSize(result.getSize())
                        .totalElements(result.getTotalElements())
                        .data(result.getContent())
                        .build())
                .build();
    }

    @GetMapping("/books/{bookId}/chapters/{chapterNumber}")
    ApiResponse<ChapterResponse> getChapterContent(@PathVariable String bookId, @PathVariable int chapterNumber) {
        return ApiResponse.<ChapterResponse>builder()
                .result(chapterService.getChapterContent(bookId, chapterNumber))
                .build();
    }

    // idea-spec BA v2 P1-02: reading:import_content cho LIBRARIAN.
    @PostMapping("/books/{bookId}/chapters")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('reading:import_content')")
    ApiResponse<ChapterResponse> createChapter(
            @PathVariable String bookId, @RequestBody @Valid ChapterCreateRequest request) {
        return ApiResponse.<ChapterResponse>builder()
                .result(chapterService.createChapter(bookId, request))
                .build();
    }

    @PostMapping(value = "/books/{bookId}/import", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('reading:import_content')")
    ApiResponse<ChapterImportResponse> importBookContent(
            @PathVariable String bookId, @RequestParam("file") MultipartFile file) {
        return ApiResponse.<ChapterImportResponse>builder()
                .result(bookContentImportService.importFile(bookId, file))
                .build();
    }
}
