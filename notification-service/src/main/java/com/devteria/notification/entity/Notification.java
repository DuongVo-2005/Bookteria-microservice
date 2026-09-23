package com.devteria.notification.entity;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Document(collection = "notifications")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification {
    @MongoId
    String id;

    @Indexed
    String userId;

    String title;

    String body;

    // isRead -> tránh Lombok "isX" boolean-getter collision với Jackson/MapStruct (lỗi đã gặp nhiều lần trong dự án).
    boolean read;

    // null = notification không gộp (VD welcome email, friend request) — có giá trị thì
    // NotificationService.createOrAggregate() sẽ tìm bản ghi UNREAD gần nhất cùng key trong
    // AGGREGATION_WINDOW để cộng dồn actorCount thay vì tạo bản ghi mới (VD nhiều người cùng
    // comment 1 bài viết nhóm trong thời gian ngắn -> gộp thành 1 thông báo).
    @Indexed
    String aggregationKey;

    @Builder.Default
    int actorCount = 1;

    Instant createdAt;
}
