package com.reportservice.domain.port.out;

import java.util.concurrent.CompletableFuture;

import com.reportservice.domain.model.ExtractedData;

public interface AiAnalysisPort {
    CompletableFuture<ExtractedData> analyzeAndStructureData(String rawData, String sourceUrl);
    CompletableFuture<String> generateSummary(String content);
    CompletableFuture<String> suggestChartType(ExtractedData data);
}
