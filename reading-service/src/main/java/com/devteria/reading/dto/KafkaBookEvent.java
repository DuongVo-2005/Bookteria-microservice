package com.devteria.reading.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Bản sao envelope của "book-events" (book-service) — copy đúng shape đã proven ở
// search-service's KafkaBookEvent, không phụ thuộc class của book-service.
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
