package com.devteria.book.entity;

import java.time.Instant;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import com.devteria.book.dto.ShelfPrivacy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

// idea-spec BA v2 §4.3: 1 document / user (khoá bằng chính userId, giống ReadingStreak bên
// reading-service), thiếu document = mặc định PUBLIC (xem ReadingListService).
@Document(collection = "shelf_settings")
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShelfSettings {
    @MongoId
    String userId;

    ShelfPrivacy privacy;

    Instant updatedAt;
}
