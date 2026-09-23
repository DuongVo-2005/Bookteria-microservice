package com.devteria.book.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec BA FEAT-01: Bước 1 + 2 của Onboarding Wizard (Bước 3 - gợi ý follow KOL - xem
// ReviewController#getActiveReaders(), không cần request body).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OnboardingPreferenceRequest {
    @NotNull(message = "ONBOARDING_CATEGORIES_MIN")
    @Size(min = 3, message = "ONBOARDING_CATEGORIES_MIN")
    List<String> categoryIds;

    @NotNull(message = "ONBOARDING_AUTHORS_MIN")
    @Size(min = 3, message = "ONBOARDING_AUTHORS_MIN")
    List<String> authorIds;
}
