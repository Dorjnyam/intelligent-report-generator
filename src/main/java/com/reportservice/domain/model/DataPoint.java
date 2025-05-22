package com.reportservice.domain.model;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataPoint {
    private String label;
    private Double value;
    private String category;
    private LocalDate date;
    private String unit;
    private String description;
}