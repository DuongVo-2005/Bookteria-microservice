package com.devteria.chat.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

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
