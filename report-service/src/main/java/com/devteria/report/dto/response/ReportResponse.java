package com.devteria.report.dto.response;

import java.time.Instant;

import com.devteria.report.dto.ReportActionType;
import com.devteria.report.dto.ReportStatus;
import com.devteria.report.dto.ReportTargetType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportResponse {
    String id;
    ReportTargetType targetType;
    String targetId;
    String reporterId;
    String reason;
    ReportStatus status;
    Instant createdAt;
    Instant reviewedAt;
    String reviewedBy;
    String adminNote;
    ReportActionType actionType;
    String targetService;
}
