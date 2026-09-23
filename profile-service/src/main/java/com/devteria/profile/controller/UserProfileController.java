package com.devteria.profile.controller;

import java.time.LocalDate;

import org.neo4j.cypherdsl.core.Condition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.devteria.profile.dto.request.ProfileCreationRequest;
import com.devteria.profile.dto.request.SearchUserRequest;
import com.devteria.profile.dto.response.ApiResponse;
import com.devteria.profile.dto.response.PageResponse;
import com.devteria.profile.dto.response.UserProfileResponse;
import com.devteria.profile.entity.UserProfile;
import com.devteria.profile.query.UserProfileQuery;
import com.devteria.profile.service.UserProfileService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserProfileController {
    UserProfileService userProfileService;

    UserProfileQuery userProfileQuery;

    @GetMapping("/users/{profileId}")
    UserProfileResponse getProfile(@PathVariable("profileId") String profileId) {
        return userProfileService.getProfile(profileId);
    }

    @DeleteMapping("/users/{profileId}")
    ResponseEntity<String> deleleProfile(@PathVariable("profileId") String id) {
        userProfileService.deleteProfile(id);
        return ResponseEntity.status(HttpStatus.OK).body("Delete Success");
    }

    @GetMapping("/users/search")
    ResponseEntity<Page<UserProfile>> searchGet(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) LocalDate dob,
            Pageable pageable) {
        Condition condition = userProfileQuery.buildCondition(firstName, lastName, city, dob);
        return ResponseEntity.status(HttpStatus.OK).body(userProfileService.searchGet(condition, pageable));
    }

    @PostMapping("/users/search")
    ApiResponse<PageResponse<UserProfileResponse>> search(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestBody SearchUserRequest request) {

        return ApiResponse.<PageResponse<UserProfileResponse>>builder()
                .result(userProfileService.search(size, page, request))
                .build();
    }

    @GetMapping("/users/my-profile")
    ApiResponse<UserProfileResponse> getMyProfile() {
        return ApiResponse.<UserProfileResponse>builder()
                .result(userProfileService.getMyProfile())
                .build();
    }

    @PutMapping("/users/my-profile")
    ApiResponse<UserProfileResponse> updateMyProfile(@RequestBody ProfileCreationRequest request) {
        return ApiResponse.<UserProfileResponse>builder()
                .result(userProfileService.updateMyProfile(request))
                .build();
    }

    @PutMapping("/users/avatar")
    ApiResponse<UserProfileResponse> updateAvatar(@RequestParam("file") MultipartFile file) {
        return ApiResponse.<UserProfileResponse>builder()
                .result(userProfileService.updateAvatar(file))
                .build();
    }
}
