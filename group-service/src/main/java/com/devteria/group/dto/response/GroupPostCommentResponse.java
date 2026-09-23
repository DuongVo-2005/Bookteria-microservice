package com.devteria.group.dto.response;

import java.time.Instant;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupPostCommentResponse {
    String id;
    String authorId;
    String authorUsername;
    String authorAvatar;
    String content;
    Instant createdAt;
}
