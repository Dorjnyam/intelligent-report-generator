package com.reportservice.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder(toBuilder = true)
public class GeneratedReport {
    private String id;
    private String fileName;
    private ReportRequest.ReportFormat format;
    private byte[] content;
    private String mimeType;
    private long sizeInBytes;
    private OffsetDateTime generatedAt;
    private String downloadUrl;
}