package com.devteria.friend.entity;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import com.devteria.friend.dto.FriendRequestStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "friend_requests")
// Chặn race condition thật ở tầng DB: sendRequest() luôn tìm-rồi-tái-dùng
// đúng 1 document cho mỗi cặp (senderId, receiverId) có thứ tự — 2 request gửi
// gần như đồng thời trước đây có thể lọt qua check existsBy() ở tầng service
// và tạo 2 document PENDING trùng nhau. Unique index này khiến lần insert thứ 2
// ném DuplicateKeyException thay vì âm thầm tạo dữ liệu trùng.
@CompoundIndex(name = "sender_receiver_unique", def = "{'senderId': 1, 'receiverId': 1}", unique = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FriendRequest {
    @MongoId
    String id;

    @Indexed
    String senderId;

    @Indexed
    String receiverId;

    FriendRequestStatus status;

    Instant createdAt;

    Instant updatedAt;
}
