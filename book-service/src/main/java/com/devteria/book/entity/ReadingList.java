package com.devteria.book.entity;

import java.time.Instant;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import com.devteria.book.dto.ReadingStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Document(collection = "reading_lists")
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingList {
    @MongoId
    String id;

    String userId;
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
