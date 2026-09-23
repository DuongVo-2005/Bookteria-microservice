package com.devteria.reading.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    CHAPTER_NOT_FOUND(1008, "Chapter not found", HttpStatus.NOT_FOUND),
    BOOKMARK_NOT_FOUND(1009, "Bookmark not found", HttpStatus.NOT_FOUND),
    HIGHLIGHT_NOT_FOUND(1010, "Highlight not found", HttpStatus.NOT_FOUND),
    INVALID_HIGHLIGHT_COLOR(1011, "Invalid highlight color", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_FILE_TYPE(1012, "Only .epub and .pdf files are supported", HttpStatus.BAD_REQUEST),
    FILE_PARSE_FAILED(1013, "Failed to parse the uploaded file", HttpStatus.BAD_REQUEST),
    SHARE_TO_POST_FAILED(1014, "Could not share this highlight as a post right now", HttpStatus.SERVICE_UNAVAILABLE),
    FIELD_REQUIRED(1015, "This field must not be blank", HttpStatus.BAD_REQUEST),
    BOOK_NOT_FOUND(1016, "Book not found", HttpStatus.NOT_FOUND),
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
