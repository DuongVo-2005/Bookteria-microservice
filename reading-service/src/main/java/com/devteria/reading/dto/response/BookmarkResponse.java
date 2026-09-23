package com.devteria.reading.dto.response;

import java.time.Instant;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookmarkResponse {
    String id;
    String bookId;
    String chapterId;
    int chapterNumber;
    String chapterTitle;
    int positionPercent;
    String snippet;
    String note;
    Instant createdAt;
}
