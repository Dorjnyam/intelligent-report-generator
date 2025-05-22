package com.reportservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportservice.infrastructure.adapter.in.web.dto.ReportGenerationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportGenerationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void generateReport_ValidRequest_Success() throws Exception {
        // Given
        ReportGenerationRequest request = new ReportGenerationRequest();
        request.setSourceUrl("https://jsonplaceholder.typicode.com/posts/1");
        request.setTitle("Test Report");

        // When & Then
        mockMvc.perform(post("/api/reports/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(request().asyncStarted()) // Waits for async
            .andReturn();

        mockMvc.perform(asyncDispatch(mockMvc.perform(post("/api/reports/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andReturn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.requestId").exists())
            .andExpect(jsonPath("$.reports").isArray());
    }


    @Test
    void generateReport_InvalidUrl_BadRequest() throws Exception {
        // Given
        ReportGenerationRequest request = new ReportGenerationRequest();
        request.setSourceUrl("invalid-url");
        request.setTitle("Test Report");

        // When & Then
        mockMvc.perform(post("/api/reports/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.sourceUrl").exists());
    }
}