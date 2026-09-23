package com.devteria.identity.entity;

import java.time.Instant;

import jakarta.persistence.*;

import com.devteria.identity.dto.OutboxEventType;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec BA GAP-01: identity-service là service DUY NHẤT dùng MySQL/JPA trong dự án (mọi
// service khác dùng Mongo @Document cho Outbox) — bảng này là bản JPA đầu tiên của pattern Outbox
// đã proven ở friend/group/post/book/reading-service, giữ đúng shape field (eventId ổn định qua
// retry, published/retryCount/createdAt/publishedAt) để OutboxPublisher hoạt động giống hệt.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    @Column(name = "id", length = 36)
    String id;

    @Column(name = "event_id", nullable = false, length = 36)
    String eventId;

    @Column(name = "aggregate_id", nullable = false)
    String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    OutboxEventType eventType;

    // KHÔNG dùng @Lob — Hibernate 6 map @Lob String sang TINYTEXT theo mặc định (tối đa 255 byte,
    // quá nhỏ cho payload JSON) trong khi migration khai TEXT, lệch ngay lúc validate schema lúc
    // boot thật (SchemaManagementException, bug thật bắt được lúc verify). Khai rõ columnDefinition
    // khớp đúng V3__create_outbox_events.sql.
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    String payload;

    @Column(name = "published", nullable = false)
    boolean published;

    @Column(name = "retry_count", nullable = false)
    int retryCount;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "published_at")
    Instant publishedAt;
}
