package com.devteria.reading.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChapterResponse {
    String id;
    String bookId;
    int chapterNumber;
    String title;
    String subtitle;
    String content;
    int readTimeMinutes;
}
