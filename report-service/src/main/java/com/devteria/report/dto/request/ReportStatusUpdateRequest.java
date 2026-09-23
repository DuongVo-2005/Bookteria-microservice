package com.devteria.report.dto.request;

import jakarta.validation.constraints.NotNull;

import com.devteria.report.dto.ReportActionType;
import com.devteria.report.dto.ReportStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportStatusUpdateRequest {
    @NotNull(message = "REPORT_INVALID_STATUS")
    ReportStatus status;

    String adminNote;

    // idea-spec BA GAP-03: bắt buộc khi status=ACTION_TAKEN, phải khớp đúng targetType của report
    // (POST->DELETE_POST, COMMENT->DELETE_COMMENT, USER->LOCK_USER, GROUP->HIDE_GROUP,
    // REVIEW->REMOVE_REVIEW) — service validate, không tin Admin gửi tuỳ ý.
    ReportActionType actionType;
}
