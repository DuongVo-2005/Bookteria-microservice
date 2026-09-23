package com.devteria.report.dto;

// idea-spec Phase 4 - "15.1 Report Target Polymorphic": 1 cơ chế report dùng chung cho mọi
// loại nội dung thay vì tạo bảng riêng cho từng loại.
public enum ReportTargetType {
    POST,
    COMMENT,
    USER,
    GROUP,
    REVIEW
}
