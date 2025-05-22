package com.reportservice.domain.port.in;

import java.util.List;
import java.util.Optional;

import com.reportservice.domain.model.GeneratedReport;

public interface ReportQueryUseCase {
    Optional<GeneratedReport> getReport(String reportId);
    List<GeneratedReport> getReportsBySourceUrl(String sourceUrl);
    void deleteReport(String reportId);
}