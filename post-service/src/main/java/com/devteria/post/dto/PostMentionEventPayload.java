package com.devteria.post.dto;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostMentionEventPayload {
    String postId;
    String authorId;
    String authorUsername;
    List<String> mentionedUserIds;
}
