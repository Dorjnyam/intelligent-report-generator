package com.reportservice.domain.port.out;

import java.util.concurrent.CompletableFuture;

import com.reportservice.domain.model.ExtractedData;

public interface DataExtractionPort {
    CompletableFuture<String> fetchRawData(String url);
    CompletableFuture<ExtractedData> extractAndAnalyzeData(String rawData, String sourceUrl);
}