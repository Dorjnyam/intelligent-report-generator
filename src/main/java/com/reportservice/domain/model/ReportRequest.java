package com.reportservice.domain.model;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder(toBuilder = true)
public class ReportRequest {
    @NonNull
    private String id;
    @NonNull
    private String sourceUrl;
    private String title;
    private ReportFormat format;
    private Map<String, Object> customParameters;
    private LocalDateTime createdAt;
    
    public enum ReportFormat {
        PDF, DOCX, BOTH
    }
}
