package com.devteria.profile.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.devteria.profile.dto.request.ProfileCreationRequest;
import com.devteria.profile.dto.request.UserIdsRequest;
import com.devteria.profile.dto.response.ApiResponse;
import com.devteria.profile.dto.response.UserProfileResponse;
import com.devteria.profile.mapper.UserProfileMapper;
import com.devteria.profile.query.UserProfileQuery;
import com.devteria.profile.repository.UserProfileRepository;
import com.devteria.profile.service.UserProfileService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalUserProfileController {
    UserProfileService userProfileService;

    UserProfileQuery userProfileQuery;

    UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;

    @PostMapping("/internal/users")
    UserProfileResponse createProfile(@RequestBody ProfileCreationRequest request) {
        UserProfileResponse userProfileResponse = userProfileService.createProfile(request);
        return userProfileResponse;
    }

    @GetMapping("/internal/users/{userId}")
    ApiResponse<UserProfileResponse> getAllProfile(@PathVariable String userId) {
        return ApiResponse.<UserProfileResponse>builder()
                .result(userProfileService.getByUserId(userId))
                .build();
    }

    @GetMapping("/internal/users/username/{username}")
    ApiResponse<UserProfileResponse> getProfileByUsername(@PathVariable String username) {
        return ApiResponse.<UserProfileResponse>builder()
                .result(userProfileService.getByUsername(username))
                .build();
    }

    @PostMapping("/internal/users/batch")
    ApiResponse<List<UserProfileResponse>> getProfilesByUserIds(@RequestBody UserIdsRequest request) {
        return ApiResponse.<List<UserProfileResponse>>builder()
                .result(userProfileService.getByUserIds(request.getUserIds()))
                .build();
    }

    // idea-spec BA v2 §2.2: identity-service gọi khi Admin deactivate 1 tài khoản. POST, không
    // phải PATCH - xem ProfileClient.anonymizeProfile() bên identity-service (Feign default
    // Client không hỗ trợ PATCH, bug thật bắt được lúc live-verify).
    @PostMapping("/internal/users/{userId}/anonymize")
    ApiResponse<Void> anonymize(@PathVariable String userId) {
        userProfileService.anonymize(userId);
        return ApiResponse.<Void>builder().message("Profile anonymized").build();
    }
}
