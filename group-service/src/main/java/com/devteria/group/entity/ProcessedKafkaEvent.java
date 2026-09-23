package com.devteria.group.entity;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Idempotency log cho consumer Kafka — copy đúng pattern book-service/chat-service/notification-service.
@Document(collection = "processed_kafka_events")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProcessedKafkaEvent {
    @MongoId
    String id;

    @Indexed(expireAfterSeconds = 604800)
    Instant processedAt;
}
