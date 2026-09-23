package com.devteria.friend.dto;

public enum OutboxEventType {
    FRIEND_REQUEST_SENT,
    FRIEND_REQUEST_ACCEPTED,
    FRIEND_REQUEST_REJECTED,
    FRIEND_REMOVED,
    FRIEND_BLOCKED,
    FRIEND_UNBLOCKED
}
