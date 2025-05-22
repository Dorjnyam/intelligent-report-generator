package com.reportservice.application.service;

import com.reportservice.domain.model.*;
import com.reportservice.domain.port.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportGenerationServiceTest {

    @Mock
    private DataExtractionPort dataExtractionPort;
    
    @Mock
    private AiAnalysisPort aiAnalysisPort;
    
    @Mock
    private ChartGenerationPort chartGenerationPort;
    
    @Mock
    private ReportRenderingPort reportRenderingPort;
    
    @Mock
    private ReportStoragePort reportStoragePort;
    
    @Mock
    private NotificationPort notificationPort;

    @InjectMocks
    private ReportGenerationService reportGenerationService;

    private ReportRequest testRequest;
    private ExtractedData testExtractedData;
    private GeneratedReport testReport;

    @BeforeEach
    void setUp() {
        testRequest = ReportRequest.builder()
            .id("test-id")
            .sourceUrl("https://example.com/data")
            .title("Test Report")
            .format(ReportRequest.ReportFormat.PDF)
            .createdAt(LocalDateTime.now())
            .build();

        testExtractedData = ExtractedData.builder()
            .sourceUrl("https://example.com/data")
            .title("Test Data")
            .summary("Test summary")
            .dataType(ExtractedData.DataType.MIXED)
            .dataPoint(DataPoint.builder()
                .label("Test Point")
                .value(100.0)
                .category("Test")
                .build())
            .build();

        testReport = GeneratedReport.builder()
            .id("report-id")
            .fileName("test-report.pdf")
            .format(ReportRequest.ReportFormat.PDF)
            .content(new byte[]{1, 2, 3})
            .mimeType("application/pdf")
            .sizeInBytes(3)
            .generatedAt(OffsetDateTime.now(ZoneOffset.UTC))
            .build();
    }

    @Test
    void generateReport_Success() {
        // Given
        when(dataExtractionPort.fetchRawData(anyString()))
            .thenReturn(CompletableFuture.completedFuture("raw data"));
        
        when(aiAnalysisPort.analyzeAndStructureData(anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(testExtractedData));
        
        when(chartGenerationPort.generateCharts(any(ExtractedData.class)))
            .thenReturn(CompletableFuture.completedFuture(List.of()));
        
        when(reportRenderingPort.renderToPdf(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(testReport));

        // When
        CompletableFuture<List<GeneratedReport>> result = 
            reportGenerationService.generateReport(testRequest);

        // Then
        assertNotNull(result);
        List<GeneratedReport> reports = result.join();
        assertEquals(1, reports.size());
        assertEquals(testReport.getId(), reports.get(0).getId());
        
        verify(reportStoragePort).saveReport(any(GeneratedReport.class));
        verify(notificationPort).notifyReportGenerated(any(GeneratedReport.class));
    }

    @Test
    void generateReport_Failure() {
        // Given
        when(dataExtractionPort.fetchRawData(anyString()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Network error")));

        // When & Then
        CompletableFuture<List<GeneratedReport>> result = 
            reportGenerationService.generateReport(testRequest);
        
        assertThrows(RuntimeException.class, result::join);
        verify(notificationPort).notifyReportFailed(eq(testRequest.getId()), anyString());
    }
}