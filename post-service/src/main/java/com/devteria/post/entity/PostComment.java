package com.devteria.post.entity;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(value = "post_comment")
public class PostComment {
    @MongoId
    String id;

    @Indexed
    String postId;

    String authorId;

    String content;

    Instant createdAt;
    Instant updatedAt;
}
