package com.devteria.group.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Bản sao envelope của "book-events" (book-service) — copy đúng shape đã proven ở
// search-service/reading-service's KafkaBookEvent, không phụ thuộc class của book-service.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KafkaBookEvent {
    String eventId;
    String eventType;
    String aggregateId;
    String payload;
    long timestamp;
}
