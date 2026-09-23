package com.devteria.book.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec BA OPS-02
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookMergeRequest {
    @NotBlank(message = "BOOK_MERGE_TARGET_REQUIRED")
    String duplicateBookId;
}
