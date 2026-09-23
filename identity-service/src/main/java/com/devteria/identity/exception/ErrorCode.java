package com.devteria.identity.exception;

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
    EMAIL_EXISTED(1009, "Email already existed", HttpStatus.BAD_REQUEST),
    GOOGLE_EMAIL_NOT_VERIFIED(1010, "Google email is not verified", HttpStatus.BAD_REQUEST),
    INVALID_OAUTH_CODE(1011, "Invalid or expired code", HttpStatus.BAD_REQUEST),
    EMAIL_IS_REQUIRED(1012, "Email must not be blank", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(1013, "Email is invalid", HttpStatus.BAD_REQUEST),
    LOCK_REASON_REQUIRED(1014, "Lock reason must not be blank", HttpStatus.BAD_REQUEST),
    LOCK_DURATION_REQUIRED(1015, "Lock duration must not be null", HttpStatus.BAD_REQUEST),
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
