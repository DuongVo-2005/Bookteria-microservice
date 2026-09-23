package com.devteria.report.entity;

import java.time.Instant;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import com.devteria.report.dto.OutboxEventType;

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

    String eventId;
    String aggregateId;
    OutboxEventType eventType;
    String payload;
    boolean published;
    int retryCount;
    Instant createdAt;
    Instant publishedAt;
}
