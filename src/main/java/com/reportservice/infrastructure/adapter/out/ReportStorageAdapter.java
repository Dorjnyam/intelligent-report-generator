package com.reportservice.infrastructure.adapter.out;
import com.reportservice.domain.model.GeneratedReport;
import com.reportservice.domain.port.out.ReportStoragePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ReportStorageAdapter implements ReportStoragePort {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final Map<String, GeneratedReport> reportStorage = new ConcurrentHashMap<>();

    @Override
    public void saveReport(GeneratedReport report) {
        String downloadUrl = generateDownloadUrl(report.getId());
        GeneratedReport reportWithUrl = report.toBuilder()
            .downloadUrl(downloadUrl)
            .build();
        
        reportStorage.put(report.getId(), reportWithUrl);
        log.info("Saved report with ID: {}", report.getId());
    }

    @Override
    public Optional<GeneratedReport> findById(String id) {
        return Optional.ofNullable(reportStorage.get(id));
    }

    @Override
    public List<GeneratedReport> findBySourceUrl(String sourceUrl) {
        return reportStorage.values().stream()
            .filter(report -> sourceUrl.equals(getSourceUrlFromReport(report)))
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        GeneratedReport removed = reportStorage.remove(id);
        if (removed != null) {
            log.info("Deleted report with ID: {}", id);
        }
    }

    @Override
    public String generateDownloadUrl(String reportId) {
        return String.format("%s/api/reports/%s/download", baseUrl, reportId);
    }

    private String getSourceUrlFromReport(GeneratedReport report) {
        // In a real implementation, you might store this separately or extract from metadata
        return ""; // This would need to be properly implemented based on your requirements
    }

    public List<GeneratedReport> findAll() {
        return List.copyOf(reportStorage.values());
    }

    public long count() {
        return reportStorage.size();
    }

    public void clear() {
        reportStorage.clear();
        log.info("Cleared all reports from storage");
    }
}