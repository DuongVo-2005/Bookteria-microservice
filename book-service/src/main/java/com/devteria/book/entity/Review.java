package com.devteria.book.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Document(collection = "reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Review {

    @MongoId
    String id;

    String bookId;

    String userId;

    int rating;

    String content;

    Instant createdAt;

    Instant updatedAt;

    // idea-spec BA FEAT-03: Spoiler Alert - nội dung bị làm mờ trên UI, FE tự xử lý hiển thị
    // (BE chỉ lưu/trả cờ, không che nội dung thật vì tác giả review vẫn cần đọc lại review của
    // chính mình khi sửa).
    @Builder.Default
    boolean hasSpoiler = false;

    // idea-spec BA FEAT-03: Helpful Upvote - lưu userId đã bấm để toggle được (bấm lại = bỏ vote)
    // và chặn 1 user vote nhiều lần cho cùng 1 review. helpfulCount denormalize riêng để Mongo
    // sort trực tiếp được (không thể Sort.by kích thước 1 Set/array mà không dùng aggregation).
    @Builder.Default
    Set<String> helpfulUserIds = new HashSet<>();

    @Builder.Default
    int helpfulCount = 0;
}
