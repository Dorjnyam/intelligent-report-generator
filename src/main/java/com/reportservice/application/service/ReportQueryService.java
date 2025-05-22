package com.reportservice.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.reportservice.domain.model.GeneratedReport;
import com.reportservice.domain.port.in.ReportQueryUseCase;
import com.reportservice.domain.port.out.ReportStoragePort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportQueryService implements ReportQueryUseCase {
    
    private final ReportStoragePort reportStoragePort;

    @Override
    public Optional<GeneratedReport> getReport(String reportId) {
        return reportStoragePort.findById(reportId);
    }

    @Override
    public List<GeneratedReport> getReportsBySourceUrl(String sourceUrl) {
        return reportStoragePort.findBySourceUrl(sourceUrl);
    }

    @Override
    public void deleteReport(String reportId) {
        reportStoragePort.deleteById(reportId);
    }
}
