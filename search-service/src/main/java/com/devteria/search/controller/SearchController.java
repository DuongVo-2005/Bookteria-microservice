package com.devteria.search.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devteria.search.dto.response.ApiResponse;
import com.devteria.search.dto.response.BookSearchResponse;
import com.devteria.search.dto.response.GroupSearchResponse;
import com.devteria.search.dto.response.PageResponse;
import com.devteria.search.dto.response.PostSearchResponse;
import com.devteria.search.service.BookSearchService;
import com.devteria.search.service.GroupSearchService;
import com.devteria.search.service.PostSearchService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SearchController {
    BookSearchService bookSearchService;
    PostSearchService postSearchService;
    GroupSearchService groupSearchService;

    @GetMapping("/books")
    public ApiResponse<PageResponse<BookSearchResponse>> getBooks(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "authorId", required = false) String authorId,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<BookSearchResponse>>builder()
                .result(bookSearchService.search(q, categoryId, authorId, sort, page, size))
                .build();
    }

    @GetMapping("/books/suggest")
    public ApiResponse<List<String>> suggest(
            @RequestParam("q") String q, @RequestParam(value = "limit", defaultValue = "8") int limit) {
        return ApiResponse.<List<String>>builder()
                .result(bookSearchService.suggest(q, limit))
                .build();
    }

    // idea-spec Phase 6 - "22.1 Search Integration"
    @GetMapping("/posts")
    public ApiResponse<PageResponse<PostSearchResponse>> getPosts(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "hashtag", required = false) String hashtag,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<PostSearchResponse>>builder()
                .result(postSearchService.search(q, hashtag, page, size))
                .build();
    }

    @GetMapping("/hashtags/trending")
    public ApiResponse<List<String>> trendingHashtags(@RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ApiResponse.<List<String>>builder()
                .result(postSearchService.trendingHashtags(limit))
                .build();
    }

    // idea-spec Phase 6 - "22.1 Search Integration" ("Group nổi bật")
    @GetMapping("/groups")
    public ApiResponse<PageResponse<GroupSearchResponse>> getGroups(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<GroupSearchResponse>>builder()
                .result(groupSearchService.search(q, category, page, size))
                .build();
    }
}
