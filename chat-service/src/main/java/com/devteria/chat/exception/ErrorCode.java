package com.devteria.chat.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_DOB(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    INVALID_VALUE(1009, "Uncategorized error", HttpStatus.BAD_REQUEST),
    CONVERSATION_NOT_FOUND(1010, "Chat conversation not found ", HttpStatus.NOT_FOUND),
    BLOCKED(1011, "You cannot message this user because one of you has blocked the other", HttpStatus.FORBIDDEN),
    MESSAGE_REQUEST_LIMIT_REACHED(
            1012, "You can only send one message until the recipient accepts your request", HttpStatus.FORBIDDEN),
    MESSAGE_REQUEST_NOT_ACCEPTED(
            1013, "You must accept this message request before you can reply", HttpStatus.FORBIDDEN),
    MESSAGE_REQUEST_REJECTED(1014, "This message request has been rejected", HttpStatus.FORBIDDEN),
    MESSAGE_REQUEST_NOT_PENDING(1015, "This message request is no longer pending", HttpStatus.BAD_REQUEST);

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
