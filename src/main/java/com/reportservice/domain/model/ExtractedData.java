package com.reportservice.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
public class ExtractedData {
    private String sourceUrl;
    private String title;
    private String summary;
    private DataType dataType;

    @Singular
    private List<DataPoint> dataPoints;
    @Singular
    private List<TextSection> textSections;

    private Map<String, Object> metadata;

    public enum DataType {
        NUMERICAL, CATEGORICAL, MIXED, TEXT_ONLY, TABLE_DATA
    }
}

