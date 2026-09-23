package com.devteria.reading.dto.response;

import java.time.Instant;

import com.devteria.reading.dto.ReadingProgressStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingProgressResponse {
    String bookId;
    String currentChapterId;
    int currentChapterIndex;
    int progressPercent;
    ReadingProgressStatus status;
    Instant lastReadAt;
}
