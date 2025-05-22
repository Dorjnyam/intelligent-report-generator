package com.reportservice.domain.port.in;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.reportservice.domain.model.GeneratedReport;
import com.reportservice.domain.model.ReportRequest;

public interface ReportGenerationUseCase {
    CompletableFuture<List<GeneratedReport>> generateReport(ReportRequest request);
    CompletableFuture<GeneratedReport> generatePdfReport(ReportRequest request);
    CompletableFuture<GeneratedReport> generateDocxReport(ReportRequest request);
}