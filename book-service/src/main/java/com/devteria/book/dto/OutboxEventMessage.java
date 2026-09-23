package com.devteria.book.dto;

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

    // Epoch millis (không dùng Instant) — Kafka JsonSerializer/JsonDeserializer dùng Jackson 2 thuần,
    // không có jackson-datatype-jsr310 trên classpath nên Instant sẽ lỗi serialize.
    long timestamp;
}
