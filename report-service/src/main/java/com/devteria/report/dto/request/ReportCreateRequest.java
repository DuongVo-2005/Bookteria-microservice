package com.devteria.report.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.devteria.report.dto.ReportTargetType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportCreateRequest {
    @NotNull(message = "REPORT_TARGET_TYPE_REQUIRED")
    ReportTargetType targetType;

    @NotBlank(message = "REPORT_TARGET_ID_REQUIRED")
    String targetId;

    @NotBlank(message = "REPORT_REASON_REQUIRED")
    String reason;
}
