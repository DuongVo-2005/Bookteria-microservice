package com.devteria.search.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

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
