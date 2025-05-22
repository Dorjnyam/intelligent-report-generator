package com.reportservice.domain.port.out;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.reportservice.domain.model.Chart;
import com.reportservice.domain.model.ExtractedData;

public interface ChartGenerationPort {
    CompletableFuture<List<Chart>> generateCharts(ExtractedData data);
    CompletableFuture<Chart> generateChart(Chart.ChartType type, List<com.reportservice.domain.model.DataPoint> dataPoints, String title);
    byte[] chartToImage(Chart chart);
}