package com.devteria.post.repository.httpclient;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.devteria.post.dto.ApiResponse;

// Gọi endpoint internal (không auth) của friend-service — chỉ lấy list userId bạn bè để
// filter feed/visibility, không cần enrich profile (xem be-report.md Phase 3, Analysis).
@FeignClient(name = "friend-service", url = "${app.service.friend.url}")
public interface FriendClient {
    @GetMapping("/friends/internal/{userId}")
    ApiResponse<List<String>> getFriendUserIds(@PathVariable String userId);
}
