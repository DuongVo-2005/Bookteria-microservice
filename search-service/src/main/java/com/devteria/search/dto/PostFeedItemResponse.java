package com.devteria.search.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Khớp JSON của post-service's PostResponse (trả về từ GET /post/feed, dùng cho reindex).
 * Field name khác PostIndexPayload (id/userId thay vì postId/authorId) vì đây là REST response
 * thật của Post entity, không phải payload event tự thiết kế - @JsonIgnoreProperties bắt buộc vì
 * PostResponse có nhiều field khác (username, bookTitle, likesCount, ...) không cần cho index.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostFeedItemResponse {
    String id;
    String content;
    String userId;
    List<String> hashtags;
    String bookId;
    String visibility;
    Instant createdDate;
}
