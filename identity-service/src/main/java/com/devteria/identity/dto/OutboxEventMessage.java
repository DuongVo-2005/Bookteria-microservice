package com.devteria.identity.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OutboxEventMessage {
    String eventId;
    OutboxEventType eventType;
    String aggregateId;

    // JSON string, giống hệt quy ước OutboxEventMessage của friend/group/post/book/reading-service
    // — payload luôn là String đã serialize sẵn (không phải Object), consumer tự
    // objectMapper.readValue() lại lần 2 theo đúng class nó cần.
    String payload;

    long timestamp;
}
