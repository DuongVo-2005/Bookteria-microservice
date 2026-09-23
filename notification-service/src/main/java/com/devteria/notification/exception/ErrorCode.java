package com.devteria.notification.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    EMAIL_SEND_FAILED(1101, "Cannot send email", HttpStatus.INTERNAL_SERVER_ERROR),
    CANNOT_SEND_EMAIL(1008, "Can not send email", HttpStatus.BAD_REQUEST),
    NOTIFICATION_NOT_FOUND(1009, "Notification not found", HttpStatus.NOT_FOUND),
    DEVICE_TOKEN_REQUIRED(1010, "Device token must not be blank", HttpStatus.BAD_REQUEST),
    DEVICE_PLATFORM_REQUIRED(1011, "Device platform is required", HttpStatus.BAD_REQUEST),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
