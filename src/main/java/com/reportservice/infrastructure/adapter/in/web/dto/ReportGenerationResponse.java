package com.reportservice.infrastructure.adapter.in.web.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReportGenerationResponse {
    private String requestId;
    private String status;
    private String message;
    private List<ReportInfo> reports;
}