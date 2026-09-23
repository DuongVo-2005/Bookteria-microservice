package com.devteria.book.dto.response;

import java.time.Instant;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OnboardingPreferenceResponse {
    boolean completed;
    List<String> categoryIds;
    List<String> authorIds;
    Instant completedAt;
}
