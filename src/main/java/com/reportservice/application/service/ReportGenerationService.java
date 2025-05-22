package com.reportservice.application.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.reportservice.domain.model.Chart;
import com.reportservice.domain.model.ExtractedData;
import com.reportservice.domain.model.GeneratedReport;
import com.reportservice.domain.model.ReportContent;
import com.reportservice.domain.model.ReportRequest;
import com.reportservice.domain.model.Table;
import com.reportservice.domain.port.in.ReportGenerationUseCase;
import com.reportservice.domain.port.out.AiAnalysisPort;
import com.reportservice.domain.port.out.ChartGenerationPort;
import com.reportservice.domain.port.out.DataExtractionPort;
import com.reportservice.domain.port.out.NotificationPort;
import com.reportservice.domain.port.out.ReportRenderingPort;
import com.reportservice.domain.port.out.ReportStoragePort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationService implements ReportGenerationUseCase {
    
    private final DataExtractionPort dataExtractionPort;
    private final AiAnalysisPort aiAnalysisPort;
    private final ChartGenerationPort chartGenerationPort;
    private final ReportRenderingPort reportRenderingPort;
    private final ReportStoragePort reportStoragePort;
    private final NotificationPort notificationPort;

    @Override
    public CompletableFuture<List<GeneratedReport>> generateReport(ReportRequest request) {
        log.info("Starting report generation for URL: {}", request.getSourceUrl());
        
        return dataExtractionPort.fetchRawData(request.getSourceUrl())
            .thenCompose(rawData -> aiAnalysisPort.analyzeAndStructureData(rawData, request.getSourceUrl()))
            .thenCompose(this::enrichWithCharts)
            .thenCompose(extractedData -> buildReportContent(extractedData, request))
            .thenCompose(content -> renderReports(content, request))
            .thenApply(reports -> {
                reports.forEach(report -> {
                    reportStoragePort.saveReport(report);
                    notificationPort.notifyReportGenerated(report);
                });
                log.info("Report generation completed for request: {}", request.getId());
                return reports;
            })
            .exceptionally(throwable -> {
                log.error("Report generation failed for request: {}", request.getId(), throwable);
                notificationPort.notifyReportFailed(request.getId(), throwable.getMessage());
                throw new RuntimeException("Report generation failed", throwable);
            });
    }

    @Override
    public CompletableFuture<GeneratedReport> generatePdfReport(ReportRequest request) {
        return generateSingleReport(request, ReportRequest.ReportFormat.PDF);
    }

    @Override
    public CompletableFuture<GeneratedReport> generateDocxReport(ReportRequest request) {
        return generateSingleReport(request, ReportRequest.ReportFormat.DOCX);
    }

    private CompletableFuture<GeneratedReport> generateSingleReport(ReportRequest request, ReportRequest.ReportFormat format) {
        ReportRequest singleFormatRequest = request.toBuilder()
            .format(format)
            .build();
            
        return generateReport(singleFormatRequest)
            .thenApply(reports -> reports.get(0));
    }

    private CompletableFuture<ExtractedData> enrichWithCharts(ExtractedData extractedData) {
        return chartGenerationPort.generateCharts(extractedData)
            .thenApply(charts -> extractedData.toBuilder()
                .metadata(extractedData.getMetadata())
                .build());
    }

    private CompletableFuture<ReportContent> buildReportContent(ExtractedData extractedData, ReportRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            List<Chart> charts = new ArrayList<>();
            List<Table> tables = new ArrayList<>();
            
            // Generate charts based on extracted data
            if (extractedData.getDataPoints() != null && !extractedData.getDataPoints().isEmpty()) {
                Chart chart = Chart.builder()
                    .title("Data Analysis")
                    .type(determineChartType(extractedData))
                    .dataPoints(extractedData.getDataPoints())
                    .description("Generated chart based on extracted data")
                    .build();
                charts.add(chart);
            }
            
            // Create tables if applicable
            if (extractedData.getDataType() == ExtractedData.DataType.TABLE_DATA) {
                Table table = createTableFromData(extractedData);
                tables.add(table);
            }

            return ReportContent.builder()
                .id(UUID.randomUUID().toString())
                .title(request.getTitle() != null ? request.getTitle() : extractedData.getTitle())
                .summary(extractedData.getSummary())
                .sourceUrl(extractedData.getSourceUrl())
                .generatedAt(LocalDateTime.now())
                .sections(extractedData.getTextSections())
                .charts(charts)
                .tables(tables)
                .build();
        });
    }

    private CompletableFuture<List<GeneratedReport>> renderReports(ReportContent content, ReportRequest request) {
        List<CompletableFuture<GeneratedReport>> renderTasks = new ArrayList<>();
        
        if (request.getFormat() == ReportRequest.ReportFormat.PDF || 
            request.getFormat() == ReportRequest.ReportFormat.BOTH) {
            renderTasks.add(reportRenderingPort.renderToPdf(content, request));
        }
        
        if (request.getFormat() == ReportRequest.ReportFormat.DOCX || 
            request.getFormat() == ReportRequest.ReportFormat.BOTH) {
            renderTasks.add(reportRenderingPort.renderToDocx(content, request));
        }
        
        return CompletableFuture.allOf(renderTasks.toArray(CompletableFuture[]::new))
            .thenApply(v -> renderTasks.stream()
                .map(CompletableFuture::join)
                .toList());
    }

    private Chart.ChartType determineChartType(ExtractedData data) {
        if (data.getDataPoints().size() <= 5) {
            return Chart.ChartType.PIE;
        } else if (data.getDataPoints().stream().anyMatch(dp -> dp.getDate() != null)) {
            return Chart.ChartType.LINE;
        } else {
            return Chart.ChartType.BAR;
        }
    }

    private Table createTableFromData(ExtractedData data) {
        List<String> headers = List.of("Label", "Value", "Category");
        List<List<String>> rows = data.getDataPoints().stream()
            .map(dp -> List.of(
                dp.getLabel() != null ? dp.getLabel() : "",
                dp.getValue() != null ? dp.getValue().toString() : "",
                dp.getCategory() != null ? dp.getCategory() : ""
            )).toList();
            
        return Table.builder()
            .title("Data Summary")
            .description("Extracted data in tabular format")
            .headers(headers)
            .rows(rows)
            .build();
    }
}