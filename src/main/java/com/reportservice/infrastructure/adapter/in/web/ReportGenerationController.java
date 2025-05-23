package com.reportservice.infrastructure.adapter.in.web;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.reportservice.domain.model.GeneratedReport;
import com.reportservice.domain.model.ReportRequest;
import com.reportservice.domain.port.in.ReportGenerationUseCase;
import com.reportservice.domain.port.in.ReportQueryUseCase;
import com.reportservice.infrastructure.adapter.in.web.dto.ReportGenerationRequest;
import com.reportservice.infrastructure.adapter.in.web.dto.ReportGenerationResponse;
import com.reportservice.infrastructure.adapter.in.web.dto.ReportInfo;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportGenerationController {

    private final ReportGenerationUseCase reportGenerationUseCase;
    private final ReportQueryUseCase reportQueryUseCase;

    @PostMapping("/generate")
    public CompletableFuture<ResponseEntity<ReportGenerationResponse>> generateReport(
            @Valid @RequestBody ReportGenerationRequest request) {
        
        log.info("Received report generation request for URL: {}", request.getSourceUrl());
        
        ReportRequest reportRequest = ReportRequest.builder()
            .id(UUID.randomUUID().toString())
            .sourceUrl(request.getSourceUrl())
            .title(request.getTitle())
            .format(request.getFormat() != null ? request.getFormat() : ReportRequest.ReportFormat.BOTH)
            .customParameters(request.getCustomParameters())
            .createdAt(LocalDateTime.now())
            .build();

        return reportGenerationUseCase.generateReport(reportRequest)
            .thenApply(reports -> {
                ReportGenerationResponse response = ReportGenerationResponse.builder()
                    .requestId(reportRequest.getId())
                    .status("SUCCESS")
                    .message("Reports generated successfully")
                    .reports(reports.stream().map(this::mapToReportInfo).toList())
                    .build();
                
                return ResponseEntity.ok(response);
            })
            .exceptionally(throwable -> {
                log.error("Failed to generate report", throwable);
                ReportGenerationResponse errorResponse = ReportGenerationResponse.builder()
                    .requestId(reportRequest.getId())
                    .status("FAILED")
                    .message("Failed to generate report: " + throwable.getMessage())
                    .build();
                
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            });
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ReportInfo> getReport(@PathVariable String reportId) {
        Optional<GeneratedReport> report = reportQueryUseCase.getReport(reportId);
        
        if (report.isPresent()) {
            ReportInfo reportInfo = mapToReportInfo(report.get());
            return ResponseEntity.ok(reportInfo);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{reportId}/download")
    public ResponseEntity<ByteArrayResource> downloadReport(@PathVariable String reportId) {
        Optional<GeneratedReport> reportOpt = reportQueryUseCase.getReport(reportId);
        
        if (reportOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        GeneratedReport report = reportOpt.get();
        ByteArrayResource resource = new ByteArrayResource(report.getContent());

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, 
            "attachment; filename=\"" + report.getFileName() + "\"");
        headers.add(HttpHeaders.CONTENT_TYPE, report.getMimeType());

        return ResponseEntity.ok()
            .headers(headers)
            .contentLength(report.getSizeInBytes())
            .contentType(MediaType.parseMediaType(report.getMimeType()))
            .body(resource);
    }

    @DeleteMapping("/{reportId}")
    public ResponseEntity<Void> deleteReport(@PathVariable String reportId) {
        reportQueryUseCase.deleteReport(reportId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-source")
    public ResponseEntity<List<ReportInfo>> getReportsBySource(@RequestParam String sourceUrl) {
        List<GeneratedReport> reports = reportQueryUseCase.getReportsBySourceUrl(sourceUrl);
        List<ReportInfo> reportInfos = reports.stream()
            .map(this::mapToReportInfo)
            .toList();
        
        return ResponseEntity.ok(reportInfos);
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