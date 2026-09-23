package com.devteria.report.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.devteria.report.dto.ReportStatus;
import com.devteria.report.dto.ReportTargetType;
import com.devteria.report.entity.Report;

@Repository
public interface ReportRepository extends MongoRepository<Report, String> {
    Page<Report> findAllByStatus(ReportStatus status, Pageable pageable);

    Page<Report> findAllByTargetType(ReportTargetType targetType, Pageable pageable);

    Page<Report> findAllByStatusAndTargetType(ReportStatus status, ReportTargetType targetType, Pageable pageable);

    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
            String reporterId, ReportTargetType targetType, String targetId, ReportStatus status);
}
