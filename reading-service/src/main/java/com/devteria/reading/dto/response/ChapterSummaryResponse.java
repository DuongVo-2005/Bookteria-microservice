package com.devteria.reading.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Danh sách chương (TOC) — KHÔNG mang content, tránh tải toàn bộ nội dung sách trong 1 response
// khi sách có nhiều chương. Muốn đọc nội dung 1 chương, gọi GET /books/{bookId}/chapters/{chapterNumber}.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChapterSummaryResponse {
    String id;
    String bookId;
    int chapterNumber;
    String title;
    String subtitle;
    int readTimeMinutes;
}
