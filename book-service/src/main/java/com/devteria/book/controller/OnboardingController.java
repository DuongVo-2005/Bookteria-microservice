package com.devteria.book.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.devteria.book.dto.request.OnboardingPreferenceRequest;
import com.devteria.book.dto.response.ApiResponse;
import com.devteria.book.dto.response.OnboardingPreferenceResponse;
import com.devteria.book.service.OnboardingService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

// idea-spec BA FEAT-01
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OnboardingController {
    OnboardingService onboardingService;

    @PostMapping("/me/onboarding")
    public ApiResponse<OnboardingPreferenceResponse> setMyPreferences(
            @RequestBody @Valid OnboardingPreferenceRequest request) {
        return ApiResponse.<OnboardingPreferenceResponse>builder()
                .result(onboardingService.setMyPreferences(request))
                .build();
    }

    @GetMapping("/me/onboarding")
    public ApiResponse<OnboardingPreferenceResponse> getMyPreferences() {
        return ApiResponse.<OnboardingPreferenceResponse>builder()
                .result(onboardingService.getMyPreferences())
                .build();
    }
}
