package com.devteria.report.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.devteria.report.dto.response.ApiResponse;

@FeignClient(name = "post-service-report", url = "${app.services.post.url}")
public interface PostClient {
    @GetMapping("/internal/posts/{postId}/exists")
    ApiResponse<Boolean> postExists(@PathVariable String postId);

    @GetMapping("/internal/comments/{commentId}/exists")
    ApiResponse<Boolean> commentExists(@PathVariable String commentId);
}
