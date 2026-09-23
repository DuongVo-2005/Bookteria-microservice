package com.devteria.notification.entity;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Idempotency log cho consumer Kafka — id = eventId của message (unique tự nhiên nhờ _id).
// TTL 7 ngày, đủ dài để chặn duplicate do Kafka redelivery (at-least-once) mà không phình vô hạn.
// Copy đúng pattern book-service/chat-service's ProcessedKafkaEvent, không tạo pattern mới.
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
