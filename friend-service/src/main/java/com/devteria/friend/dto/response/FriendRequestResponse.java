package com.devteria.friend.dto.response;

import java.time.Instant;

import com.devteria.friend.dto.FriendRequestStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FriendRequestResponse {
    String id;
    String senderId;
    String senderUsername;
    String senderAvatar;
    String receiverId;
    FriendRequestStatus status;
    Instant createdAt;
}
