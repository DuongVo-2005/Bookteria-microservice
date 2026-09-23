package com.devteria.report.entity;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import com.devteria.report.dto.ReportActionType;
import com.devteria.report.dto.ReportStatus;
import com.devteria.report.dto.ReportTargetType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reports")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Report {
    @MongoId
    String id;

    @Indexed
    ReportTargetType targetType;

    @Indexed
    String targetId;

    @Indexed
    String reporterId;

    String reason;

    @Indexed
    ReportStatus status;

    Instant createdAt;

    Instant reviewedAt;

    String reviewedBy;

    String adminNote;

    // idea-spec BA GAP-03: chỉ set khi status=ACTION_TAKEN. targetService suy ra từ targetType
    // (server tự tính, không tin client) — lưu lại để audit/truy vết, không phải input.
    ReportActionType actionType;

    String targetService;
}
