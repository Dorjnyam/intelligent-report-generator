package com.reportservice.infrastructure.adapter.in.graphql;


import com.reportservice.domain.model.GeneratedReport;
import com.reportservice.domain.model.ReportRequest;
import com.reportservice.domain.port.in.ReportGenerationUseCase;
import com.reportservice.domain.port.in.ReportQueryUseCase;
import com.reportservice.infrastructure.adapter.in.web.dto.ReportInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ReportGraphQLController {

    private final ReportGenerationUseCase reportGenerationUseCase;
    private final ReportQueryUseCase reportQueryUseCase;

    @MutationMapping
    public CompletableFuture<List<ReportInfo>> generateReport(
            @Argument String sourceUrl,
            @Argument String title,
            @Argument String format) {
        
        log.info("GraphQL: Received report generation request for URL: {}", sourceUrl);
        
        ReportRequest.ReportFormat reportFormat = ReportRequest.ReportFormat.BOTH;
        if (format != null) {
            try {
                reportFormat = ReportRequest.ReportFormat.valueOf(format.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid format provided: {}, using default: BOTH", format);
            }
        }
        
        ReportRequest reportRequest = ReportRequest.builder()
            .id(UUID.randomUUID().toString())
            .sourceUrl(sourceUrl)
            .title(title)
            .format(reportFormat)
            .createdAt(LocalDateTime.now())
            .build();

        return reportGenerationUseCase.generateReport(reportRequest)
            .thenApply(reports -> reports.stream()
                .map(this::mapToReportInfo)
                .toList());
    }

    @QueryMapping
    public Optional<ReportInfo> report(@Argument String id) {
        return reportQueryUseCase.getReport(id)
            .map(this::mapToReportInfo);
    }

    @QueryMapping
    public List<ReportInfo> reportsBySource(@Argument String sourceUrl) {
        return reportQueryUseCase.getReportsBySourceUrl(sourceUrl)
            .stream()
            .map(this::mapToReportInfo)
            .toList();
    }

    private ReportInfo mapToReportInfo(GeneratedReport report) {
        return ReportInfo.builder()
            .id(report.getId())
            .fileName(report.getFileName())
            .format(report.getFormat().name())
            .sizeInBytes(report.getSizeInBytes())
            .generatedAt(report.getGeneratedAt())
            .downloadUrl(report.getDownloadUrl())
            .build();
    }
}
