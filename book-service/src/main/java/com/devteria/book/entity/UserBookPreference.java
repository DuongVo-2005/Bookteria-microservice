package com.devteria.book.entity;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec BA FEAT-01: sở thích khai báo lúc Onboarding Wizard (3-step) — dùng làm tín hiệu cold-
// start cho RecommendationService thay cho trending chung chung, trước khi user có ReadingList.
// _id = userId luôn (1 user chỉ có 1 bản ghi sở thích) -> upsert bằng save() trực tiếp, không cần
// findByUserId trước.
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_book_preferences")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserBookPreference {
    @MongoId
    String userId;

    List<String> categoryIds;

    List<String> authorIds;

    Instant completedAt;
}
