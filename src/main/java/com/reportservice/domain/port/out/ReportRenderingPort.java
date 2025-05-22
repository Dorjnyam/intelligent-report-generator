package com.reportservice.domain.port.out;

import java.util.concurrent.CompletableFuture;

import com.reportservice.domain.model.GeneratedReport;
import com.reportservice.domain.model.ReportContent;
import com.reportservice.domain.model.ReportRequest;

public interface ReportRenderingPort {
    CompletableFuture<GeneratedReport> renderToPdf(ReportContent content, ReportRequest request);
    CompletableFuture<GeneratedReport> renderToDocx(ReportContent content, ReportRequest request);
    CompletableFuture<GeneratedReport> renderToLatexPdf(ReportContent content, ReportRequest request);
}