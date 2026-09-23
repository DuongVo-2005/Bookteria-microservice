package com.devteria.book.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Bản sao riêng của book-service cho envelope Kafka phát ra từ reading-service (topic
// "reading-progress-events") — không phụ thuộc class Java của reading-service, chỉ cần khớp
// đúng shape JSON thật (đúng pattern KafkaBookEvent bên search-service/FriendEventMessage bên
// chat-service: consumer tự định nghĩa DTO khớp JSON, không import module của producer).
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingSyncEventMessage {
    String eventId;
    String eventType;
    String aggregateId;
    String payload;
    long timestamp;
}
