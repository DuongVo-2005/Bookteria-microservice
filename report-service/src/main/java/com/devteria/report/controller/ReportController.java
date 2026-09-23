package com.devteria.report.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.devteria.report.dto.ReportStatus;
import com.devteria.report.dto.ReportTargetType;
import com.devteria.report.dto.request.ReportCreateRequest;
import com.devteria.report.dto.request.ReportStatusUpdateRequest;
import com.devteria.report.dto.response.ApiResponse;
import com.devteria.report.dto.response.PageResponse;
import com.devteria.report.dto.response.ReportResponse;
import com.devteria.report.service.ReportService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportController {
    ReportService reportService;

    @PostMapping
    ApiResponse<ReportResponse> createReport(@RequestBody @Valid ReportCreateRequest request) {
        return ApiResponse.<ReportResponse>builder()
                .result(reportService.createReport(request))
                .build();
    }

    @GetMapping
    ApiResponse<PageResponse<ReportResponse>> listReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<ReportResponse>>builder()
                .result(reportService.listReports(status, targetType, page, size))
                .build();
    }

    @GetMapping("/{id}")
    ApiResponse<ReportResponse> getReport(@PathVariable String id) {
        return ApiResponse.<ReportResponse>builder()
                .result(reportService.getReport(id))
                .build();
    }

    @PatchMapping("/{id}/status")
    ApiResponse<ReportResponse> updateStatus(
            @PathVariable String id, @RequestBody @Valid ReportStatusUpdateRequest request) {
        return ApiResponse.<ReportResponse>builder()
                .result(reportService.updateStatus(id, request))
                .build();
    }
}
