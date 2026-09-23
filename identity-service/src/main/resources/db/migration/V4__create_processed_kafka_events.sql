-- idea-spec BA GAP-03: idempotency cho consumer Kafka "admin-commands" (bản JPA đầu tiên của
-- pattern ProcessedKafkaEvent đã proven ở các service Mongo khác).
CREATE TABLE `processed_kafka_events` (
    `id` VARCHAR(36) NOT NULL,
    `processed_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_processed_kafka_events_processed_at` ON `processed_kafka_events` (`processed_at`);
