package com.devteria.report.dto;

// idea-spec BA GAP-03: hành động thực thi khi Admin duyệt report ở trạng thái ACTION_TAKEN —
// đúng 1:1 với ReportTargetType (POST->DELETE_POST, COMMENT->DELETE_COMMENT, USER->LOCK_USER,
// GROUP->HIDE_GROUP, REVIEW->REMOVE_REVIEW). Vẫn là field admin tự truyền (không tự suy ra hoàn
// toàn phía server) để khớp đúng tinh thần spec, nhưng service validate khớp targetType — không
// tin Admin gửi sai actionType cho 1 report loại khác.
public enum ReportActionType {
    DELETE_POST,
    DELETE_COMMENT,
    LOCK_USER,
    HIDE_GROUP,
    REMOVE_REVIEW
}
