package com.devteria.report.mapper;

import org.mapstruct.Mapper;

import com.devteria.report.dto.response.ReportResponse;
import com.devteria.report.entity.Report;

@Mapper(componentModel = "spring")
public interface ReportMapper {
    ReportResponse toReportResponse(Report report);
}
