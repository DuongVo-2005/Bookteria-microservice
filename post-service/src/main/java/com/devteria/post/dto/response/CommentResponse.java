package com.devteria.post.dto.response;

import java.time.Instant;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentResponse {
    String id;
    String postId;
    String authorId;
    String authorUsername;
    String content;
    Instant createdAt;
    Instant updatedAt;
}
