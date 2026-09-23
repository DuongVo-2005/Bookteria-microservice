package com.devteria.post.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.devteria.post.dto.ApiResponse;
import com.devteria.post.dto.response.UserProfileResponse;

@FeignClient(name = "profile-service", url = "${app.service.profile.url}")
public interface ProfileClient {
    @GetMapping("/internal/users/{userId}")
    ApiResponse<UserProfileResponse> getProfile(@PathVariable("userId") String userId);

    // idea-spec Phase 3 - "10.1 Rich Text & Mention" - resolve @username trong content thành
    // userId thật. Cùng endpoint friend-service đã dùng (profile-service's
    // InternalUserProfileController, PUBLIC_ENDPOINTS permitAll, không cần auth).
    @GetMapping("/internal/users/username/{username}")
    ApiResponse<UserProfileResponse> getProfileByUsername(@PathVariable("username") String username);
}
