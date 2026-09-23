package com.devteria.post.dto;

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
    Object payload;

    // Epoch millis (không dùng Instant) — khớp đúng lý do đã ghi ở friend-service/group-service's
    // OutboxEventMessage: Kafka JsonSerializer/JsonDeserializer dùng Jackson 2 thuần.
    long timestamp;
}
