package com.devteria.book.dto.response;

import java.util.List;

import com.devteria.book.exception.error.BatchImportError;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BatchImportResponse {
    int success;
    int failed;
    int skipped;
    List<BatchImportError> errors;
}
