package com.devteria.identity.entity;

import java.time.Instant;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Idempotency log cho consumer Kafka — bản JPA đầu tiên của pattern đã proven ở
// book/chat/notification/group/reading/post-service (Mongo). identity-service KHÔNG có TTL index
// tự động như Mongo — dọn bằng @Scheduled riêng (xem AdminCommandConsumer's cleanup job) thay vì
// tự hết hạn.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "processed_kafka_events")
public class ProcessedKafkaEvent {
    @Id
    @Column(name = "id", length = 36)
    String id;

    @Column(name = "processed_at", nullable = false)
    Instant processedAt;
}
