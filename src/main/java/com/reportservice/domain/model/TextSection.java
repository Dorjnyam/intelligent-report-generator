package com.reportservice.domain.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TextSection {
    private String title;
    private String content;
    private Integer order;
    private SectionType type;
    
    public enum SectionType {
        HEADER, PARAGRAPH, BULLET_POINT, QUOTE, CONCLUSION
    }
}
