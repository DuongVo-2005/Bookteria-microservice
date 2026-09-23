package com.devteria.group.dto;

// idea-spec BA OPS-01: luồng duyệt bài viết trong nhóm kín (Group.requireApproval).
public enum GroupPostStatus {
    PUBLISHED,
    PENDING_APPROVAL,
    REJECTED
}
