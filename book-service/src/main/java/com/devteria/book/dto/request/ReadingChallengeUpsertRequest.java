package com.devteria.book.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec BA FEAT-02
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingChallengeUpsertRequest {
    @NotNull(message = "READING_CHALLENGE_YEAR_INVALID")
    @Min(value = 1, message = "READING_CHALLENGE_YEAR_INVALID")
    Integer year;

    @NotNull(message = "READING_CHALLENGE_TARGET_INVALID")
    @Min(value = 1, message = "READING_CHALLENGE_TARGET_INVALID")
    Integer targetBooks;
}
