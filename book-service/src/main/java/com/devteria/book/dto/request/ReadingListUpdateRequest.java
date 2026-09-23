package com.devteria.book.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

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
public class ReadingListUpdateRequest {
    ReadingStatus status;

    Integer currentPage;

    @Min(value = 0, message = "READING_LIST_PROGRESS_PERCENT_INVALID")
    @Max(value = 100, message = "READING_LIST_PROGRESS_PERCENT_INVALID")
    Integer progressPercent;

    String notes;
}
