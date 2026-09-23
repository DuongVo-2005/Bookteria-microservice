package com.devteria.book.dto.response;

import java.time.Instant;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewResponse {

    String id;

    String bookId;

    String userId;

    Integer rating;

    String content;

    Instant createdAt;

    Instant updatedAt;

    boolean hasSpoiler;

    int helpfulCount;

    // idea-spec BA FEAT-03: derived theo user hiện tại lúc trả response, không map trực tiếp từ
    // Review entity (Review.helpfulUserIds không nên lộ nguyên danh sách userId ra response).
    boolean helpfulByMe;
}
