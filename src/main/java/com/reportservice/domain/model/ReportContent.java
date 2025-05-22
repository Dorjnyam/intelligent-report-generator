package com.reportservice.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReportContent {
    private String id;
    private String title;
    private String summary;
    private String sourceUrl;
    private LocalDateTime generatedAt;
    
    @Singular
    private List<TextSection> sections;
    
    @Singular
    private List<Chart> charts;
    
    @Singular
    private List<Table> tables;
}