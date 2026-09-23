package com.devteria.reading.entity;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "highlights")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Highlight {
    @MongoId
    String id;

    @Indexed
    String userId;

    @Indexed
    String bookId;

    String chapterId;

    int chapterNumber;

    String chapterTitle;

    String selectedText;

    // Plain String, not an enum — FE's 5 fixed values (yellow/green/pink/blue/
    // purple) are validated in the service layer instead, avoiding a Jackson
    // enum-case mismatch (a known repeat gotcha in this repo).
    String color;

    String note;

    Instant createdAt;

    // idea-spec BA v2 §3.1 Offline Sync Batch: idempotency key do client tự sinh cho 1 lần tạo
    // (null khi tạo online trực tiếp như cũ) - chặn tạo trùng nếu client gửi lại đúng 1 batch 2
    // lần (vd network timeout nhưng server đã xử lý xong). Xem HighlightService.createHighlight().
    @Indexed
    String clientId;
}
