package com.devteria.chat.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.devteria.chat.configuration.AuthenticationRequestInterceptor;
import com.devteria.chat.dto.response.ApiResponse;
import com.devteria.chat.dto.response.FriendStatusResponse;

@FeignClient(
        name = "friend-service",
        url = "${app.services.friend.url}",
        configuration = AuthenticationRequestInterceptor.class)
public interface FriendClient {
    @GetMapping("/friends/status/{otherUserId}")
    ApiResponse<FriendStatusResponse> getFriendshipStatus(@PathVariable String otherUserId);
}
