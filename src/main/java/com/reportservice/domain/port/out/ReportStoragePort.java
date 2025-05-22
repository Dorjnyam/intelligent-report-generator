package com.reportservice.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.reportservice.domain.model.GeneratedReport;

public interface ReportStoragePort {
    void saveReport(GeneratedReport report);
    Optional<GeneratedReport> findById(String id);
    List<GeneratedReport> findBySourceUrl(String sourceUrl);
    void deleteById(String id);
    String generateDownloadUrl(String reportId);
}