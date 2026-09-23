-- idea-spec BA GAP-01: Outbox pattern cho welcome-email (trước đây publish thẳng
-- kafkaTemplate.send(), mất event nếu Kafka down đúng lúc đăng ký).
CREATE TABLE `outbox_events` (
    `id` VARCHAR(36) NOT NULL,
    `event_id` VARCHAR(36) NOT NULL,
    `aggregate_id` VARCHAR(255) NOT NULL,
    `event_type` VARCHAR(64) NOT NULL,
    `payload` TEXT NOT NULL,
    `published` BIT(1) NOT NULL DEFAULT b'0',
    `retry_count` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME(6) NOT NULL,
    `published_at` DATETIME(6) NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_outbox_events_published_created` ON `outbox_events` (`published`, `created_at`);
CREATE INDEX `idx_outbox_events_published_published_at` ON `outbox_events` (`published`, `published_at`);
