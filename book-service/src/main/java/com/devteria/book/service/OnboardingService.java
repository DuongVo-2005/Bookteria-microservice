package com.devteria.book.service;

import java.time.Instant;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.devteria.book.dto.request.OnboardingPreferenceRequest;
import com.devteria.book.dto.response.OnboardingPreferenceResponse;
import com.devteria.book.entity.UserBookPreference;
import com.devteria.book.exception.AppException;
import com.devteria.book.exception.ErrorCode;
import com.devteria.book.repository.AuthorRepository;
import com.devteria.book.repository.CategoryRepository;
import com.devteria.book.repository.UserBookPreferenceRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

// idea-spec BA FEAT-01: Bước 1 + 2 của Onboarding Wizard (chọn category/author yêu thích) — dùng
// làm tín hiệu cold-start cho RecommendationService. Xem OnboardingPreferenceRequest cho lý do
// Bước 3 (gợi ý follow KOL) không thuộc service này.
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OnboardingService {
    UserBookPreferenceRepository userBookPreferenceRepository;
    CategoryRepository categoryRepository;
    AuthorRepository authorRepository;

    public OnboardingPreferenceResponse setMyPreferences(OnboardingPreferenceRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        for (String categoryId : request.getCategoryIds()) {
            if (!categoryRepository.existsById(categoryId)) {
                throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
            }
        }
        for (String authorId : request.getAuthorIds()) {
            if (!authorRepository.existsById(authorId)) {
                throw new AppException(ErrorCode.AUTHOR_NOT_FOUND);
            }
        }

        UserBookPreference preference = UserBookPreference.builder()
                .userId(userId)
                .categoryIds(request.getCategoryIds())
                .authorIds(request.getAuthorIds())
                .completedAt(Instant.now())
                .build();
        userBookPreferenceRepository.save(preference);

        return toResponse(preference);
    }

    public OnboardingPreferenceResponse getMyPreferences() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        return userBookPreferenceRepository
                .findById(userId)
                .map(this::toResponse)
                .orElseGet(() -> OnboardingPreferenceResponse.builder()
                        .completed(false)
                        .categoryIds(java.util.List.of())
                        .authorIds(java.util.List.of())
                        .build());
    }

    private OnboardingPreferenceResponse toResponse(UserBookPreference preference) {
        return OnboardingPreferenceResponse.builder()
                .completed(true)
                .categoryIds(preference.getCategoryIds())
                .authorIds(preference.getAuthorIds())
                .completedAt(preference.getCompletedAt())
                .build();
    }
}
