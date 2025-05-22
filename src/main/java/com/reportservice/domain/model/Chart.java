package com.reportservice.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class Chart {
    private String title;
    private ChartType type;
    private String xAxisLabel;
    private String yAxisLabel;
    
    @Singular
    private List<DataPoint> dataPoints;
    
    private byte[] imageData;
    private String description;
    
    public enum ChartType {
        BAR, PIE, LINE, SCATTER, HISTOGRAM
    }
}
