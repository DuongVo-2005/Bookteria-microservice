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
@Document(collection = "bookmarks")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Bookmark {
    @MongoId
    String id;

    @Indexed
    String userId;

    @Indexed
    String bookId;

    String chapterId;

    int chapterNumber;

    String chapterTitle;

    int positionPercent;

    String snippet;

    String note;

    Instant createdAt;

    // idea-spec BA v2 §3.1 Offline Sync Batch: idempotency key do client tự sinh cho 1 lần tạo
    // (null khi tạo online trực tiếp như cũ) - chặn tạo trùng nếu client gửi lại đúng 1 batch 2
    // lần. Xem BookmarkService.createBookmark().
    @Indexed
    String clientId;
}
