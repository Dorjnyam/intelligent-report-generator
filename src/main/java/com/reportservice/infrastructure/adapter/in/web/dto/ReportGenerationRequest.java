package com.reportservice.infrastructure.adapter.in.web.dto;

import com.reportservice.domain.model.ReportRequest;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

@Data
public class ReportGenerationRequest {
    
    @NotBlank(message = "Source URL is required")
    @Pattern(regexp = "^https?://.*", message = "Source URL must be a valid HTTP/HTTPS URL")
    private String sourceUrl;
    
    private String title;
    
    private ReportRequest.ReportFormat format;
    
    private Map<String, Object> customParameters;
}