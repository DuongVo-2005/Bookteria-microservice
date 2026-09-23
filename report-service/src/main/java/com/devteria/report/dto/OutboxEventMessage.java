package com.devteria.report.dto;

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
    String payload;
    long timestamp;
}
