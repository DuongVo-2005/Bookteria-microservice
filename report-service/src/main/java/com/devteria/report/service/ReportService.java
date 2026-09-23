package com.devteria.report.service;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.devteria.report.dto.OutboxEventType;
import com.devteria.report.dto.ReportActionType;
import com.devteria.report.dto.ReportStatus;
import com.devteria.report.dto.ReportTargetType;
import com.devteria.report.dto.request.ReportCreateRequest;
import com.devteria.report.dto.request.ReportStatusUpdateRequest;
import com.devteria.report.dto.response.PageResponse;
import com.devteria.report.dto.response.ReportResponse;
import com.devteria.report.entity.Report;
import com.devteria.report.exception.AppException;
import com.devteria.report.exception.ErrorCode;
import com.devteria.report.mapper.ReportMapper;
import com.devteria.report.repository.ReportRepository;
import com.devteria.report.repository.httpclient.BookClient;
import com.devteria.report.repository.httpclient.GroupClient;
import com.devteria.report.repository.httpclient.PostClient;
import com.devteria.report.repository.httpclient.ProfileClient;

import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ReportService {
    ReportRepository reportRepository;
    ReportMapper reportMapper;
    OutboxEventService outboxEventService;
    ProfileClient profileClient;
    PostClient postClient;
    GroupClient groupClient;
    BookClient bookClient;

    // idea-spec BA GAP-03: mỗi targetType chỉ có đúng 1 actionType hợp lệ và 1 service sở hữu.
    // Dùng cho cả validate actionType lúc ACTION_TAKEN lẫn tính targetService (server tự suy ra,
    // không tin client).
    private static final Map<ReportTargetType, ReportActionType> EXPECTED_ACTION = Map.of(
            ReportTargetType.POST, ReportActionType.DELETE_POST,
            ReportTargetType.COMMENT, ReportActionType.DELETE_COMMENT,
            ReportTargetType.USER, ReportActionType.LOCK_USER,
            ReportTargetType.GROUP, ReportActionType.HIDE_GROUP,
            ReportTargetType.REVIEW, ReportActionType.REMOVE_REVIEW);

    private static final Map<ReportTargetType, String> TARGET_SERVICE = Map.of(
            ReportTargetType.POST, "post-service",
            ReportTargetType.COMMENT, "post-service",
            ReportTargetType.USER, "identity-service",
            ReportTargetType.GROUP, "group-service",
            ReportTargetType.REVIEW, "book-service");

    // idea-spec Phase 4 - "15. Report System" + BA GAP-03: bất kỳ user đã login report được 1
    // target (Post/Comment/User/Group/Review). GAP-03 thêm "validate nhẹ" targetId có thật tồn
    // tại qua Feign trước khi cho lưu PENDING (tránh spam report vào ID giả) — chỉ check tồn tại,
    // không check quyền xem (report-service không cần biết reporter có xem được nội dung không).
    public ReportResponse createReport(ReportCreateRequest request) {
        String reporterId = currentUserId();

        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                reporterId, request.getTargetType(), request.getTargetId(), ReportStatus.PENDING)) {
            throw new AppException(ErrorCode.REPORT_ALREADY_PENDING);
        }

        if (!targetExists(request.getTargetType(), request.getTargetId())) {
            throw new AppException(ErrorCode.REPORT_TARGET_NOT_FOUND);
        }

        Report report = Report.builder()
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reporterId(reporterId)
                .reason(request.getReason())
                .status(ReportStatus.PENDING)
                .createdAt(Instant.now())
                .build();
        reportRepository.save(report);

        return reportMapper.toReportResponse(report);
    }

    private boolean targetExists(ReportTargetType targetType, String targetId) {
        try {
            return switch (targetType) {
                case POST -> Boolean.TRUE.equals(postClient.postExists(targetId).getResult());
                case COMMENT ->
                    Boolean.TRUE.equals(postClient.commentExists(targetId).getResult());
                case GROUP ->
                    Boolean.TRUE.equals(groupClient.groupExists(targetId).getResult());
                case REVIEW ->
                    Boolean.TRUE.equals(bookClient.reviewExists(targetId).getResult());
                case USER -> {
                    try {
                        profileClient.getProfile(targetId);
                        yield true;
                    } catch (FeignException.NotFound e) {
                        yield false;
                    }
                }
            };
        } catch (Exception e) {
            // Service sở hữu targetId tạm thời không gọi được (mạng, service down...) - chấp
            // nhận rủi ro false-negative thấp hơn chặn hẳn tính năng report khi 1 service khác
            // đang gặp sự cố không liên quan. Log lại để biết.
            log.warn("Failed to validate report target existence. targetType={}, targetId={}", targetType, targetId, e);
            return true;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<ReportResponse> listReports(
            ReportStatus status, ReportTargetType targetType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Report> pageData;
        if (status != null && targetType != null) {
            pageData = reportRepository.findAllByStatusAndTargetType(status, targetType, pageable);
        } else if (status != null) {
            pageData = reportRepository.findAllByStatus(status, pageable);
        } else if (targetType != null) {
            pageData = reportRepository.findAllByTargetType(targetType, pageable);
        } else {
            pageData = reportRepository.findAll(pageable);
        }

        return PageResponse.<ReportResponse>builder()
                .currentPage(page)
                .pageSize(size)
                .totalPages(pageData.getTotalPages())
                .totalElements(pageData.getTotalElements())
                .data(pageData.getContent().stream()
                        .map(reportMapper::toReportResponse)
                        .toList())
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ReportResponse getReport(String id) {
        return reportMapper.toReportResponse(getReportOrThrow(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ReportResponse updateStatus(String id, ReportStatusUpdateRequest request) {
        if (request.getStatus() == ReportStatus.PENDING) {
            throw new AppException(ErrorCode.REPORT_INVALID_STATUS);
        }
        Report report = getReportOrThrow(id);

        // idea-spec BA GAP-03: ACTION_TAKEN phải kèm actionType đúng với targetType của report -
        // đây là điểm khác biệt duy nhất so với REVIEWED/DISMISSED (chỉ đổi trạng thái, không
        // thực thi gì thêm ở service khác).
        if (request.getStatus() == ReportStatus.ACTION_TAKEN) {
            if (request.getActionType() == null) {
                throw new AppException(ErrorCode.REPORT_ACTION_TYPE_REQUIRED);
            }
            if (request.getActionType() != EXPECTED_ACTION.get(report.getTargetType())) {
                throw new AppException(ErrorCode.REPORT_ACTION_TYPE_MISMATCH);
            }
            report.setActionType(request.getActionType());
            report.setTargetService(TARGET_SERVICE.get(report.getTargetType()));
        }

        report.setStatus(request.getStatus());
        report.setAdminNote(request.getAdminNote());
        report.setReviewedAt(Instant.now());
        report.setReviewedBy(currentUserId());
        reportRepository.save(report);
        log.info(
                "[AUDIT] admin={} action=UPDATE_REPORT_STATUS target=report:{} newStatus={}",
                report.getReviewedBy(),
                id,
                report.getStatus());

        if (report.getStatus() == ReportStatus.ACTION_TAKEN) {
            outboxEventService.recordEvent(
                    report.getId(),
                    OutboxEventType.ADMIN_COMMAND_EXECUTE,
                    Map.of(
                            "reportId", report.getId(),
                            "targetService", report.getTargetService(),
                            "actionType", report.getActionType().name(),
                            "targetType", report.getTargetType().name(),
                            "targetId", report.getTargetId()));
        }

        return reportMapper.toReportResponse(report);
    }

    private Report getReportOrThrow(String id) {
        return reportRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
    }

    private String currentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
