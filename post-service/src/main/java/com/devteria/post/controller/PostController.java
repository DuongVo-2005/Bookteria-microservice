package com.devteria.post.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.devteria.post.dto.ApiResponse;
import com.devteria.post.dto.request.CommentRequest;
import com.devteria.post.dto.request.PostRequest;
import com.devteria.post.dto.request.ShareRequest;
import com.devteria.post.dto.response.CommentResponse;
import com.devteria.post.dto.response.PageResponse;
import com.devteria.post.dto.response.PostResponse;
import com.devteria.post.service.PostService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostController {

    PostService postService;

    @PostMapping("/")
    ApiResponse<PostResponse> createPost(@RequestBody @Valid PostRequest request) {
        return ApiResponse.<PostResponse>builder()
                .result(postService.createPost(request))
                .build();
    }

    @GetMapping("/my-posts")
    ApiResponse<PageResponse<PostResponse>> getMyPost(
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<PostResponse>>builder()
                .result(postService.getMyPosts(page, size))
                .build();
    }

    // idea-spec Phase 3 - "5. Global Feed"
    @GetMapping("/feed")
    ApiResponse<PageResponse<PostResponse>> getGlobalFeed(
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<PostResponse>>builder()
                .result(postService.getGlobalFeed(page, size))
                .build();
    }

    @GetMapping("/feed/friends")
    ApiResponse<PageResponse<PostResponse>> getFriendFeed(
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<PostResponse>>builder()
                .result(postService.getFriendFeed(page, size))
                .build();
    }

    // idea-spec Phase 6 - "22. Personalized Feed"
    @GetMapping("/feed/personalized")
    ApiResponse<PageResponse<PostResponse>> getPersonalizedFeed(
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<PostResponse>>builder()
                .result(postService.getPersonalizedFeed(page, size))
                .build();
    }

    @GetMapping("/users/{userId}/posts")
    ApiResponse<PageResponse<PostResponse>> getUserPosts(
            @PathVariable String userId,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<PostResponse>>builder()
                .result(postService.getUserPosts(userId, page, size))
                .build();
    }

    @GetMapping("/{postId}")
    ApiResponse<PostResponse> getPost(@PathVariable String postId) {
        return ApiResponse.<PostResponse>builder()
                .result(postService.getPost(postId))
                .build();
    }

    // idea-spec Phase 4 - "12. Post Moderation": tác giả hoặc platform ADMIN.
    @DeleteMapping("/{postId}")
    ApiResponse<Void> deletePost(@PathVariable String postId) {
        postService.deletePost(postId);
        return ApiResponse.<Void>builder().message("Post deleted").build();
    }

    // idea-spec Phase 3 - "6. Post Like": toggle - gọi lại lần 2 tự unlike.
    @PostMapping("/{postId}/like")
    ApiResponse<PostResponse> toggleLike(@PathVariable String postId) {
        return ApiResponse.<PostResponse>builder()
                .result(postService.toggleLike(postId))
                .build();
    }

    // idea-spec Phase 3 - "7. Post Comment"
    @PostMapping("/{postId}/comments")
    ApiResponse<CommentResponse> addComment(@PathVariable String postId, @RequestBody @Valid CommentRequest request) {
        return ApiResponse.<CommentResponse>builder()
                .result(postService.addComment(postId, request))
                .build();
    }

    @GetMapping("/{postId}/comments")
    ApiResponse<PageResponse<CommentResponse>> getComments(
            @PathVariable String postId,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<CommentResponse>>builder()
                .result(postService.getComments(postId, page, size))
                .build();
    }

    @PutMapping("/comments/{commentId}")
    ApiResponse<CommentResponse> updateComment(
            @PathVariable String commentId, @RequestBody @Valid CommentRequest request) {
        return ApiResponse.<CommentResponse>builder()
                .result(postService.updateComment(commentId, request))
                .build();
    }

    @DeleteMapping("/comments/{commentId}")
    ApiResponse<Void> deleteComment(@PathVariable String commentId) {
        postService.deleteComment(commentId);
        return ApiResponse.<Void>builder().message("Comment deleted").build();
    }

    // idea-spec Phase 3 - "10.2 Share / Re-post"
    @PostMapping("/{postId}/share")
    ApiResponse<PostResponse> sharePost(
            @PathVariable String postId, @RequestBody(required = false) ShareRequest request) {
        return ApiResponse.<PostResponse>builder()
                .result(postService.sharePost(
                        postId,
                        request != null ? request : ShareRequest.builder().build()))
                .build();
    }
}
