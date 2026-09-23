package com.devteria.group.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    GROUP_NOT_FOUND(1008, "Group not found", HttpStatus.NOT_FOUND),
    NOT_GROUP_MEMBER(1009, "You are not a member of this group", HttpStatus.FORBIDDEN),
    ALREADY_GROUP_MEMBER(1010, "You are already a member of this group", HttpStatus.BAD_REQUEST),
    GROUP_OWNER_CANNOT_LEAVE(
            1011, "Group owner cannot leave the group, transfer ownership first", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_GROUP_PERMISSION(1012, "You do not have permission for this group action", HttpStatus.FORBIDDEN),
    CANNOT_CHANGE_OWNER_ROLE(1013, "Cannot change the role of the group owner", HttpStatus.BAD_REQUEST),
    GROUP_POST_NOT_FOUND(1014, "Group post not found", HttpStatus.NOT_FOUND),
    GROUP_POST_COMMENT_NOT_FOUND(1015, "Comment not found", HttpStatus.NOT_FOUND),
    GROUP_MEMBER_NOT_FOUND(1016, "Group member not found", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(1017, "User not existed", HttpStatus.NOT_FOUND),
    BOOK_NOT_FOUND(1018, "Book not found", HttpStatus.NOT_FOUND),
    CANNOT_TRANSFER_TO_SELF(1019, "You are already the owner of this group", HttpStatus.BAD_REQUEST),
    GROUP_POST_NOT_PENDING(1020, "This post is not pending approval", HttpStatus.BAD_REQUEST),
    GROUP_FROZEN(1021, "This group is frozen - posting is temporarily disabled", HttpStatus.FORBIDDEN),
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
