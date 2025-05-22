package com.reportservice.infrastructure.adapter.in.web.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class ReportInfo {
    private String id;
    private String fileName;
    private String format;
    private long sizeInBytes;
    private OffsetDateTime generatedAt; // ✅ Changed from LocalDateTime
    private String downloadUrl;
}
