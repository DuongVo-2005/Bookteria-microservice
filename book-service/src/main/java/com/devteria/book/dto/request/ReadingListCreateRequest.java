package com.devteria.book.dto.request;

import jakarta.validation.constraints.NotBlank;

import com.devteria.book.dto.ReadingStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingListCreateRequest {
    @NotBlank
    String bookId;

    ReadingStatus status;
}
