package com.reportservice.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class Table {
    private String title;
    private String description;
    
    @Singular
    private List<String> headers;
    
    @Singular
    private List<List<String>> rows;
}