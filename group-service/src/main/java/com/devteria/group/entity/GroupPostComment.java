package com.devteria.group.entity;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "group_post_comments")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupPostComment {
    @MongoId
    String id;

    @Indexed
    String postId;

    String authorId;

    String content;

    Instant createdAt;
}
