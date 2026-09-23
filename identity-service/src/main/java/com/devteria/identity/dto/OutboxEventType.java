package com.devteria.identity.dto;

public enum OutboxEventType {
    WELCOME_EMAIL,

    // idea-spec BA v2 §2.2: Admin Password Reset - tái dùng đúng topic "notification-delivery" +
    // WelcomeEmailConsumer có sẵn bên notification-service (consumer đó không rẽ nhánh theo
    // eventType, chỉ đọc subject/body/recipient từ NotificationEvent) - không cần sửa gì bên đó.
    PASSWORD_RESET_EMAIL
}
