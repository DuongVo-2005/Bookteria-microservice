package com.devteria.search.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Envelope Kafka chung, dùng cho cả "post-events" và "group-events" (cùng shape
// OutboxEventMessage của mọi service khác trong project) - khác KafkaBookEvent chỉ vì đặt tên
// theo topic gốc, không phải khác cấu trúc.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KafkaEventMessage {
    String eventId;
    String eventType;
    String aggregateId;
    String payload;
    long timestamp;
}
