package com.devteria.group.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import com.devteria.group.dto.request.GroupPostCommentCreateRequest;
import com.devteria.group.dto.request.GroupPostCreateRequest;
import com.devteria.group.dto.request.GroupPostRejectRequest;
import com.devteria.group.dto.response.ApiResponse;
import com.devteria.group.dto.response.GroupPostCommentResponse;
import com.devteria.group.dto.response.GroupPostResponse;
import com.devteria.group.dto.response.PageResponse;
import com.devteria.group.service.GroupPostService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/groups/{groupId}/posts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupPostController {
    GroupPostService groupPostService;

    @GetMapping
    ApiResponse<PageResponse<GroupPostResponse>> listPosts(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<GroupPostResponse> result = groupPostService.listPosts(groupId, page, size);
        return ApiResponse.<PageResponse<GroupPostResponse>>builder()
                .result(toPageResponse(result))
                .build();
    }

    // idea-spec BA OPS-01: hàng chờ duyệt — OWNER/ADMIN của group.
    @GetMapping("/pending")
    ApiResponse<PageResponse<GroupPostResponse>> listPendingPosts(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<GroupPostResponse> result = groupPostService.listPendingPosts(groupId, page, size);
        return ApiResponse.<PageResponse<GroupPostResponse>>builder()
                .result(toPageResponse(result))
                .build();
    }

    @PostMapping
    ApiResponse<GroupPostResponse> createPost(
            @PathVariable String groupId, @RequestBody @Valid GroupPostCreateRequest request) {
        return ApiResponse.<GroupPostResponse>builder()
                .result(groupPostService.createPost(groupId, request))
                .build();
    }

    @PostMapping("/{postId}/like")
    ApiResponse<Void> toggleLike(@PathVariable String groupId, @PathVariable String postId) {
        groupPostService.toggleLike(groupId, postId);
        return ApiResponse.<Void>builder().message("Like toggled").build();
    }

    @PostMapping("/{postId}/comments")
    ApiResponse<GroupPostCommentResponse> addComment(
            @PathVariable String groupId,
            @PathVariable String postId,
            @RequestBody @Valid GroupPostCommentCreateRequest request) {
        return ApiResponse.<GroupPostCommentResponse>builder()
                .result(groupPostService.addComment(groupId, postId, request))
                .build();
    }

    // idea-spec BA OPS-01
    @PostMapping("/{postId}/approve")
    ApiResponse<GroupPostResponse> approvePost(@PathVariable String groupId, @PathVariable String postId) {
        return ApiResponse.<GroupPostResponse>builder()
                .result(groupPostService.approvePost(groupId, postId))
                .build();
    }

    @PostMapping("/{postId}/reject")
    ApiResponse<Void> rejectPost(
            @PathVariable String groupId,
            @PathVariable String postId,
            @RequestBody(required = false) GroupPostRejectRequest request) {
        groupPostService.rejectPost(groupId, postId, request);
        return ApiResponse.<Void>builder().message("Post rejected").build();
    }

    @DeleteMapping("/{postId}")
    ApiResponse<Void> deletePost(@PathVariable String groupId, @PathVariable String postId) {
        groupPostService.deletePost(groupId, postId);
        return ApiResponse.<Void>builder().message("Post deleted").build();
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    ApiResponse<Void> deleteComment(
            @PathVariable String groupId, @PathVariable String postId, @PathVariable String commentId) {
        groupPostService.deleteComment(groupId, postId, commentId);
        return ApiResponse.<Void>builder().message("Comment deleted").build();
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return PageResponse.<T>builder()
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .data(page.getContent())
                .build();
    }
}
