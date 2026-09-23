package com.devteria.friend.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.devteria.friend.dto.response.ApiResponse;
import com.devteria.friend.dto.response.UserProfileResponse;

@FeignClient(name = "profile-service", url = "${app.services.profile.url}")
public interface ProfileClient {
    @GetMapping("/internal/users/{userId}")
    ApiResponse<UserProfileResponse> getProfile(@PathVariable String userId);

    @GetMapping("/internal/users/username/{username}")
    ApiResponse<UserProfileResponse> getProfileByUsername(@PathVariable String username);
}
