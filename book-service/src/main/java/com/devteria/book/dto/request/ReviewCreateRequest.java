package com.devteria.book.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewCreateRequest {

    @NotNull(message = "REVIEW_RATING_REQUIRED")
    @Min(value = 1, message = "REVIEW_RATING_INVALID")
    @Max(value = 5, message = "REVIEW_RATING_INVALID")
    Integer rating;

    String content;

    // idea-spec BA FEAT-03
    boolean hasSpoiler;
}
