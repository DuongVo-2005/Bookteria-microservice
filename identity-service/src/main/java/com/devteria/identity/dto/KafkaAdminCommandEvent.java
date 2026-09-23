package com.devteria.identity.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Bản sao envelope của "admin-commands" (report-service) — payload String đã pre-serialize.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KafkaAdminCommandEvent {
    String eventId;
    String eventType;
    String aggregateId;
    String payload;
    long timestamp;
}
