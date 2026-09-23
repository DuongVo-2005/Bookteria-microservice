package com.devteria.friend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    USER_NOT_FOUND(1008, "User not existed", HttpStatus.NOT_FOUND),
    CANNOT_FRIEND_YOURSELF(1009, "You cannot send a friend request to yourself", HttpStatus.BAD_REQUEST),
    FRIEND_REQUEST_ALREADY_PENDING(
            1010, "A friend request is already pending between these users", HttpStatus.BAD_REQUEST),
    ALREADY_FRIENDS(1011, "You are already friends with this user", HttpStatus.BAD_REQUEST),
    FRIEND_REQUEST_NOT_FOUND(1012, "Friend request not found", HttpStatus.NOT_FOUND),
    FRIEND_REQUEST_NOT_PENDING(1013, "This friend request is no longer pending", HttpStatus.BAD_REQUEST),
    NOT_FRIENDS(1014, "You are not friends with this user", HttpStatus.BAD_REQUEST),
    ALREADY_BLOCKED(1015, "You have already blocked this user", HttpStatus.BAD_REQUEST),
    NOT_BLOCKED(1016, "You have not blocked this user", HttpStatus.BAD_REQUEST),
    CANNOT_BLOCK_YOURSELF(1017, "You cannot block yourself", HttpStatus.BAD_REQUEST),
    BLOCKED(
            1018,
            "You cannot send a friend request to a blocked user or a user who blocked you",
            HttpStatus.BAD_REQUEST),
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
