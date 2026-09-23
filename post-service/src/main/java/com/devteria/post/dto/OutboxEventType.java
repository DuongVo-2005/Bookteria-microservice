package com.devteria.post.dto;

public enum OutboxEventType {
    POST_MENTIONED,
    // idea-spec Phase 6 - "22.1 Search Integration"
    POST_CREATED,
    POST_DELETED
}
