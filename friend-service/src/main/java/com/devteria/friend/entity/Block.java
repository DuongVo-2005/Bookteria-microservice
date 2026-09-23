package com.devteria.friend.entity;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
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
@Document(collection = "blocks")
// Cùng lý do như FriendRequest — chặn race condition 2 block-request đồng thời
// tạo trùng document cho cùng 1 cặp (blockerId, blockedId).
@CompoundIndex(name = "blocker_blocked_unique", def = "{'blockerId': 1, 'blockedId': 1}", unique = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Block {
    @MongoId
    String id;

    @Indexed
    String blockerId;

    @Indexed
    String blockedId;

    Instant createdAt;
}
