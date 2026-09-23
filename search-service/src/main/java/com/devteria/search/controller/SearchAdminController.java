package com.devteria.search.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devteria.search.dto.response.ApiResponse;
import com.devteria.search.dto.response.ReindexResponse;
import com.devteria.search.service.BookIndexService;
import com.devteria.search.service.GroupIndexService;
import com.devteria.search.service.PostIndexService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SearchAdminController {
    BookIndexService bookIndexService;
    PostIndexService postIndexService;
    GroupIndexService groupIndexService;

    @PostMapping("/admin/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<ReindexResponse> reindex() {
        int totalIndexed = bookIndexService.reindexAll();
        return ApiResponse.<ReindexResponse>builder()
                .result(new ReindexResponse(totalIndexed))
                .build();
    }

    // idea-spec Phase 6 - "22.1 Search Integration"
    @PostMapping("/admin/reindex/posts")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<ReindexResponse> reindexPosts() {
        int totalIndexed = postIndexService.reindexAll();
        return ApiResponse.<ReindexResponse>builder()
                .result(new ReindexResponse(totalIndexed))
                .build();
    }

    @PostMapping("/admin/reindex/groups")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<ReindexResponse> reindexGroups() {
        int totalIndexed = groupIndexService.reindexAll();
        return ApiResponse.<ReindexResponse>builder()
                .result(new ReindexResponse(totalIndexed))
                .build();
    }
}
