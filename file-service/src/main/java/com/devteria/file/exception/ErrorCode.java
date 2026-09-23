package com.devteria.file.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    FILE_NOT_FOUND(1008, "File not found", HttpStatus.NOT_FOUND),
    FILE_EMPTY(1009, "File must not be empty", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_FILE_TYPE(1010, "Only jpg, jpeg, png, gif and webp files are supported", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(1011, "File exceeds the maximum allowed size (10MB)", HttpStatus.BAD_REQUEST),
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