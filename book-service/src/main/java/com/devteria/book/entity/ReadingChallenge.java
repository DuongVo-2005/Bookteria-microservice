package com.devteria.book.entity;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec BA FEAT-02: "Đọc X cuốn trong năm Y" — completedBooks tăng tự động khi 1 ReadingList
// của user chuyển sang COMPLETED trong đúng năm Y (xem ReadingListService).
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reading_challenges")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingChallenge {
    @MongoId
    String id;

    @Indexed
    String userId;

    int year;

    int targetBooks;

    @Builder.Default
    int completedBooks = 0;

    Instant createdAt;

    Instant updatedAt;
}
