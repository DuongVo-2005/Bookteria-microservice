package com.devteria.reading.entity;

import java.time.Instant;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import com.devteria.reading.dto.OutboxEventType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Document(collection = "outbox_events")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OutboxEvent {
    @MongoId
    String id;

    // Idempotency key ổn định qua các lần retry publish — sinh 1 lần lúc recordEvent(), không sinh lại lúc publish.
    String eventId;

    String aggregateType;

    String aggregateId;

    OutboxEventType eventType;

    String payload;

    boolean published;

    int retryCount;

    Instant createdAt;

    Instant publishedAt;
}
