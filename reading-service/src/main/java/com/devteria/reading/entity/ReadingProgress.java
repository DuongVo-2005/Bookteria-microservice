package com.devteria.reading.entity;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import com.devteria.reading.dto.ReadingProgressStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reading_progress")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingProgress {
    @MongoId
    String id;

    @Indexed
    String userId;

    @Indexed
    String bookId;

    String currentChapterId;

    int currentChapterIndex;

    int progressPercent;

    ReadingProgressStatus status;

    Instant lastReadAt;
}
