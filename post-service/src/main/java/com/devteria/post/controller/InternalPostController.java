package com.devteria.post.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.devteria.post.dto.ApiResponse;
import com.devteria.post.repository.PostCommentRepository;
import com.devteria.post.repository.PostRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

// idea-spec BA GAP-03: report-service cần "validate nhẹ" targetId có thật tồn tại trước khi cho
// lưu Report ở trạng thái PENDING (tránh spam ID giả) — 2 endpoint permitAll, service-to-service,
// cùng pattern /internal/** đã có sẵn ở profile-service/friend-service.
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalPostController {
    PostRepository postRepository;
    PostCommentRepository postCommentRepository;

    @GetMapping("/internal/posts/{postId}/exists")
    ApiResponse<Boolean> postExists(@PathVariable String postId) {
        return ApiResponse.<Boolean>builder()
                .result(postRepository.existsById(postId))
                .build();
    }

    @GetMapping("/internal/comments/{commentId}/exists")
    ApiResponse<Boolean> commentExists(@PathVariable String commentId) {
        return ApiResponse.<Boolean>builder()
                .result(postCommentRepository.existsById(commentId))
                .build();
    }
}
