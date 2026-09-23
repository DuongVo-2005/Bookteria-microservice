package com.devteria.notification.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Envelope chung cho cả "friend-events" và "group-events" — đúng shape thật
// friend-service/group-service publish (eventId/eventType/aggregateId/payload/timestamp),
// payload là JSON đã pre-serialize thành String (không phải object lồng), giống bản sao
// đã có ở chat-service (com.devteria.chat.dto.FriendEventMessage) — tái dùng đúng tên cho nhất quán.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FriendEventMessage {
    String eventId;
    String eventType;
    String aggregateId;
    String payload;
    long timestamp;
}
