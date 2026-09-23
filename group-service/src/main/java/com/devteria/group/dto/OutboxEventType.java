package com.devteria.group.dto;

public enum OutboxEventType {
    GROUP_MEMBER_JOINED,
    GROUP_MEMBER_LEFT,
    GROUP_ROLE_CHANGED,
    GROUP_POST_CREATED,
    GROUP_POST_DELETED,
    GROUP_COMMENT_CREATED,
    // idea-spec Phase 6 - "22.1 Search Integration"
    GROUP_CREATED,
    GROUP_DELETED
}
