package com.devteria.notification.entity;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import lombok.*;
import lombok.experimental.FieldDefaults;

// idea-spec Phase 1 - "1.4 Device Token / Push Notification": chỉ lưu trữ FCM token, CHƯA
// tích hợp gửi push thật (cần Firebase Admin SDK + credentials thật, ngoài phạm vi "chuẩn bị
// nền tảng" của mục này) - xem be-report.md phần Known Issues.
@Document(collection = "device_tokens")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeviceToken {
    @MongoId
    String id;

    @Indexed
    String userId;

    @Indexed(unique = true)
    String token;

    String platform;

    Instant createdAt;
}
