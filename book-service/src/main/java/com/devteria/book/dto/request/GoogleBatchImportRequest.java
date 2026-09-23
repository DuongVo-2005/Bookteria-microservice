package com.devteria.book.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GoogleBatchImportRequest {
    @NotBlank(message = "BOOK_SEARCH_KEYWORD_REQUIRED")
    String query;

    @Min(1)
    @Max(40)
    @Builder.Default
    int maxResults = 20;

    @Min(0)
    @Builder.Default
    int startIndex = 0;
}
