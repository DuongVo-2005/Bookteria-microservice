package com.devteria.chat.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.*;
import lombok.experimental.FieldDefaults;

// friend-service publish FRIEND_REQUEST_SENT/ACCEPTED/REJECTED/REMOVED bằng nguyên object
// FriendRequestResponse (có thêm id/status/createdAt mà class này không khai) — thiếu
// @JsonIgnoreProperties khiến ObjectMapper mặc định (FAIL_ON_UNKNOWN_PROPERTIES=true) ném
// UnrecognizedPropertyException, bị nuốt trong catch(Exception) của FriendEventConsumer,
// khiến 4/6 loại friend-event không bao giờ relay được qua Socket.IO (chỉ FRIEND_BLOCKED/
// UNBLOCKED dùng payload gọn hơn nên trước đây không lộ ra).
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FriendEventPayload {

    String blockerId;

    String blockedId;

    String senderId;

    String senderUsername;

    String senderAvatar;

    String receiverId;
}
