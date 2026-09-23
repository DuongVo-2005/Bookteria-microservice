package com.devteria.report.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    REPORT_NOT_FOUND(1008, "Report not found", HttpStatus.NOT_FOUND),
    REPORT_TARGET_TYPE_REQUIRED(1009, "Target type is required", HttpStatus.BAD_REQUEST),
    REPORT_TARGET_ID_REQUIRED(1010, "Target id must not be blank", HttpStatus.BAD_REQUEST),
    REPORT_REASON_REQUIRED(1011, "Reason must not be blank", HttpStatus.BAD_REQUEST),
    REPORT_ALREADY_PENDING(1012, "You already have a pending report for this target", HttpStatus.BAD_REQUEST),
    REPORT_INVALID_STATUS(1013, "Invalid report status", HttpStatus.BAD_REQUEST),
    REPORT_TARGET_NOT_FOUND(1014, "The reported content or user no longer exists", HttpStatus.BAD_REQUEST),
    REPORT_ACTION_TYPE_REQUIRED(
            1015, "actionType is required when marking a report as ACTION_TAKEN", HttpStatus.BAD_REQUEST),
    REPORT_ACTION_TYPE_MISMATCH(1016, "actionType does not match this report's targetType", HttpStatus.BAD_REQUEST),
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
