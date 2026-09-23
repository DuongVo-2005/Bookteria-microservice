package com.devteria.search.dto;

import java.time.Instant;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Khớp JSON của post-service's PostIndexEventPayload (payload của POST_CREATED trong
 * OutboxEventMessage). Tự định nghĩa riêng, không import class của post-service.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostIndexPayload {
    String postId;
    String content;
    String authorId;
    List<String> hashtags;
    String bookId;
    String visibility;
    Instant createdDate;
}
