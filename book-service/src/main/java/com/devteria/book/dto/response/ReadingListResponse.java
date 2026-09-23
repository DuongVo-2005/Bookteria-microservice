package com.devteria.book.dto.response;

import java.time.Instant;

import com.devteria.book.dto.ReadingStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingListResponse {
    String id;
    String bookId;
    ReadingStatus status;
    Integer currentPage;
    Integer progressPercent;
    String notes;
    Instant startedAt;
    Instant completedAt;
    Instant createdAt;
    Instant updatedAt;
}
