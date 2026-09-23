package com.devteria.reading.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChapterCreateRequest {
    @Positive
    int chapterNumber;

    @NotBlank
    String title;

    String subtitle;

    @NotBlank
    String content;

    int readTimeMinutes;
}
