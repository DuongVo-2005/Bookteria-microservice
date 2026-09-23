package com.devteria.report.mapper;

import com.devteria.report.dto.response.ReportResponse;
import com.devteria.report.entity.Report;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Microsoft)"
)
@Component
public class ReportMapperImpl implements ReportMapper {

    @Override
    public ReportResponse toReportResponse(Report report) {
        if ( report == null ) {
            return null;
        }

        ReportResponse.ReportResponseBuilder reportResponse = ReportResponse.builder();

        reportResponse.id( report.getId() );
        reportResponse.targetType( report.getTargetType() );
        reportResponse.targetId( report.getTargetId() );
        reportResponse.reporterId( report.getReporterId() );
        reportResponse.reason( report.getReason() );
        reportResponse.status( report.getStatus() );
        reportResponse.createdAt( report.getCreatedAt() );
        reportResponse.reviewedAt( report.getReviewedAt() );
        reportResponse.reviewedBy( report.getReviewedBy() );
        reportResponse.adminNote( report.getAdminNote() );
        reportResponse.actionType( report.getActionType() );
        reportResponse.targetService( report.getTargetService() );

        return reportResponse.build();
    }
}
