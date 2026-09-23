package com.devteria.notification.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Khớp đúng field thật của friend-service's FriendRequestResponse (payload thật của
// FRIEND_REQUEST_SENT/ACCEPTED/REJECTED/REMOVED) — @JsonIgnoreProperties(ignoreUnknown = true)
// bắt buộc phải có vì payload thật còn field id/status/createdAt không cần dùng ở đây
// (thiếu annotation này sẽ ném UnrecognizedPropertyException, xem bug đã fix ở
// chat-service's FriendEventPayload cùng nguyên nhân).
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FriendRequestEventPayload {
    String senderId;
    String senderUsername;
    String senderAvatar;
    String receiverId;
}
