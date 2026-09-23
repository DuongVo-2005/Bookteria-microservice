package com.devteria.post.entity;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
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
@Document(value = "post_like")
@CompoundIndex(name = "post_user_unique", def = "{'postId': 1, 'userId': 1}", unique = true)
public class PostLike {
    @MongoId
    String id;

    String postId;

    String userId;

    Instant createdAt;
}
