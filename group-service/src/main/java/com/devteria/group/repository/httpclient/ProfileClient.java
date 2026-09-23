package com.devteria.group.repository.httpclient;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.devteria.group.dto.request.UserIdsRequest;
import com.devteria.group.dto.response.ApiResponse;
import com.devteria.group.dto.response.UserProfileResponse;

@FeignClient(name = "profile-service", url = "${app.services.profile.url}")
public interface ProfileClient {
    @GetMapping("/internal/users/{userId}")
    ApiResponse<UserProfileResponse> getProfile(@PathVariable String userId);

    @PostMapping("/internal/users/batch")
    ApiResponse<List<UserProfileResponse>> getProfiles(@RequestBody UserIdsRequest request);
}
