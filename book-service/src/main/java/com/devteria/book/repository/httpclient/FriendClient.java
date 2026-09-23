package com.devteria.book.repository.httpclient;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.devteria.book.dto.response.ApiResponse;

// idea-spec BA v2 §4.3 Shelf Privacy (FRIENDS_ONLY): gọi endpoint internal (permitAll) của
// friend-service - cùng client/endpoint post-service đã dùng để lọc feed theo bạn bè (xem
// be-report.md Phase 3), chỉ lấy list userId, không enrich profile.
@FeignClient(name = "friend-service", url = "${app.service.friend.url}")
public interface FriendClient {
    @GetMapping("/friends/internal/{userId}")
    ApiResponse<List<String>> getFriendUserIds(@PathVariable String userId);
}
