package com.reportservice.infrastructure.adapter.out;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.reportservice.domain.model.DataPoint;
import com.reportservice.domain.model.ExtractedData;
import com.reportservice.domain.model.TextSection;
import com.reportservice.domain.port.out.AiAnalysisPort;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiAnalysisAdapter implements AiAnalysisPort {

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String model;

    // private final ObjectMapper objectMapper;
    private OpenAiService openAiService;

    private OpenAiService getOpenAiService() {
        if (openAiService == null && !openaiApiKey.isEmpty()) {
            openAiService = new OpenAiService(openaiApiKey);
            log.info("Initialized OpenAiService with provided API key.");
        }
        return openAiService;
    }

    @Override
    public CompletableFuture<ExtractedData> analyzeAndStructureData(String rawData, String sourceUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (getOpenAiService() != null) {
                    return analyzeWithOpenAI(rawData, sourceUrl);
                } else {
                    log.warn("OpenAI service not available, using fallback analysis");
                    return analyzeWithFallback(rawData, sourceUrl);
                }
            } catch (Exception e) {
                log.error("Error in AI analysis, falling back to basic analysis", e);
                return analyzeWithFallback(rawData, sourceUrl);
            }
        });
    }

    @Override
    public CompletableFuture<String> generateSummary(String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (getOpenAiService() != null) {
                    return generateSummaryWithOpenAI(content);
                } else {
                    return generateFallbackSummary(content);
                }
            } catch (Exception e) {
                log.error("Error generating summary", e);
                return generateFallbackSummary(content);
            }
        });
    }

    @Override
    public CompletableFuture<String> suggestChartType(ExtractedData data) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (getOpenAiService() != null) {
                    return suggestChartTypeWithOpenAI(data);
                } else {
                    return suggestChartTypeWithFallback(data);
                }
            } catch (Exception e) {
                log.error("Error suggesting chart type", e);
                return suggestChartTypeWithFallback(data);
            }
        });
    }

    private ExtractedData analyzeWithOpenAI(String rawData, String sourceUrl) {
        String prompt = buildAnalysisPrompt(rawData);

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(Arrays.asList(
                        new ChatMessage("system",
                                "You are a data analyst AI. Analyze the provided data and extract meaningful insights, "
                                        +
                                        "numerical data points, and structure the content for reporting purposes. " +
                                        "Return your analysis in a structured format."),
                        new ChatMessage("user", prompt)))
                .maxTokens(2000)
                .temperature(0.3)
                .build();

        String response = getOpenAiService().createChatCompletion(request)
                .getChoices().get(0).getMessage().getContent();

        return parseAiResponse(response, rawData, sourceUrl);
    }

    private String generateSummaryWithOpenAI(String content) {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(Arrays.asList(
                        new ChatMessage("system",
                                "You are a professional report writer. Create a concise, professional summary " +
                                        "of the provided content in 2-3 sentences."),
                        new ChatMessage("user", "Please summarize this content: " + truncateContent(content, 3000))))
                .maxTokens(200)
                .temperature(0.3)
                .build();

        return getOpenAiService().createChatCompletion(request)
                .getChoices().get(0).getMessage().getContent();
    }

    private String suggestChartTypeWithOpenAI(ExtractedData data) {
        String dataDescription = buildDataDescription(data);

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(Arrays.asList(
                        new ChatMessage("system",
                                "You are a data visualization expert. Based on the provided data characteristics, " +
                                        "suggest the most appropriate chart type. Respond with only one of: BAR, PIE, LINE, SCATTER, HISTOGRAM"),
                        new ChatMessage("user", "What chart type would be best for this data? " + dataDescription)))
                .maxTokens(50)
                .temperature(0.1)
                .build();

        return getOpenAiService().createChatCompletion(request)
                .getChoices().get(0).getMessage().getContent().trim().toUpperCase();
    }

    private ExtractedData analyzeWithFallback(String rawData, String sourceUrl) {
        log.info("Using fallback AI analysis for: {}", sourceUrl);

        List<DataPoint> dataPoints = extractNumericDataFallback(rawData);
        List<TextSection> textSections = extractTextSectionsFallback(rawData);

        String title = extractTitleFallback(rawData);
        String summary = generateFallbackSummary(rawData);

        return ExtractedData.builder()
                .sourceUrl(sourceUrl)
                .title(title)
                .summary(summary)
                .dataType(determineDataTypeFallback(dataPoints, textSections))
                .dataPoints(dataPoints)
                .textSections(textSections)
                .build();
    }

    private String generateFallbackSummary(String content) {
        String truncated = truncateContent(content, 500);
        int wordCount = truncated.split("\\s+").length;
        int sentenceCount = truncated.split("[.!?]+").length;

        return String.format("Document contains approximately %d words and %d sentences. " +
                "Content includes structured data and textual information suitable for analysis and reporting.",
                wordCount, sentenceCount);
    }

    private String suggestChartTypeWithFallback(ExtractedData data) {
        if (data.getDataPoints().isEmpty()) {
            return "BAR";
        }

        int dataPointCount = data.getDataPoints().size();
        boolean hasTimeData = data.getDataPoints().stream()
                .anyMatch(dp -> dp.getDate() != null);
        boolean hasCategoricalData = data.getDataPoints().stream()
                .anyMatch(dp -> dp.getCategory() != null && !dp.getCategory().isEmpty());

        if (hasTimeData) {
            return "LINE";
        } else if (dataPointCount <= 5 && hasCategoricalData) {
            return "PIE";
        } else if (dataPointCount > 20) {
            return "HISTOGRAM";
        } else {
            return "BAR";
        }
    }

    private String buildAnalysisPrompt(String rawData) {
        String truncatedData = truncateContent(rawData, 4000);
        return String.format("""
                Please analyze the following data and provide insights:

                Data: %s

                Please identify:
                1. Key numerical data points with labels
                2. Main topics and themes
                3. Important text sections
                4. Data type (numerical, categorical, mixed, text-only)
                5. Suggested chart types for visualization
                6. A brief summary of the content""",
                truncatedData);
    }

    private String buildDataDescription(ExtractedData data) {
        StringBuilder desc = new StringBuilder();
        desc.append("Data points: ").append(data.getDataPoints().size());

        if (!data.getDataPoints().isEmpty()) {
            boolean hasCategories = data.getDataPoints().stream()
                    .anyMatch(dp -> dp.getCategory() != null);
            boolean hasTime = data.getDataPoints().stream()
                    .anyMatch(dp -> dp.getDate() != null);

            desc.append(", Has categories: ").append(hasCategories);
            desc.append(", Has time data: ").append(hasTime);
            desc.append(", Data type: ").append(data.getDataType());
        }

        return desc.toString();
    }

    private ExtractedData parseAiResponse(String response, String rawData, String sourceUrl) {
        // This is a simplified parser - in a real implementation, you'd want more
        // robust parsing
        List<DataPoint> dataPoints = new ArrayList<>();
        List<TextSection> textSections = new ArrayList<>();

        // Extract numerical data mentioned in the response
        Pattern numberPattern = Pattern.compile("(\\w+[\\s\\w]*?):\\s*(\\d+(?:\\.\\d+)?)");
        Matcher matcher = numberPattern.matcher(response);

        while (matcher.find()) {
            String label = matcher.group(1).trim();
            Double value = Double.valueOf(matcher.group(2));

            DataPoint dataPoint = DataPoint.builder()
                    .label(label)
                    .value(value)
                    .category("AI Extracted")
                    .build();
            dataPoints.add(dataPoint);
        }

        // If no structured data found, fall back to original extraction
        if (dataPoints.isEmpty()) {
            dataPoints = extractNumericDataFallback(rawData);
        }

        // Extract title from response or generate one
        String title = extractTitleFromResponse(response);
        if (title == null || title.isEmpty()) {
            title = extractTitleFallback(rawData);
        }

        // Create text sections from the analysis
        String[] paragraphs = response.split("\n\n");
        for (int i = 0; i < paragraphs.length; i++) {
            if (!paragraphs[i].trim().isEmpty()) {
                TextSection section = TextSection.builder()
                        .content(paragraphs[i].trim())
                        .order(i)
                        .type(TextSection.SectionType.PARAGRAPH)
                        .build();
                textSections.add(section);
            }
        }

        return ExtractedData.builder()
                .sourceUrl(sourceUrl)
                .title(title)
                .summary(generateFallbackSummary(response))
                .dataType(determineDataTypeFallback(dataPoints, textSections))
                .dataPoints(dataPoints)
                .textSections(textSections)
                .build();
    }

    private List<DataPoint> extractNumericDataFallback(String rawData) {
        List<DataPoint> dataPoints = new ArrayList<>();
        Pattern numberPattern = Pattern.compile("\\b(\\d+(?:\\.\\d+)?)\\b");
        Matcher matcher = numberPattern.matcher(rawData);

        int count = 0;
        while (matcher.find() && count < 20) {
            try {
                Double value = Double.valueOf(matcher.group(1));
                DataPoint dataPoint = DataPoint.builder()
                        .label("Value " + (count + 1))
                        .value(value)
                        .category("Extracted")
                        .build();
                dataPoints.add(dataPoint);
                count++;
            } catch (NumberFormatException e) {
                // Skip invalid numbers
            }
        }

        return dataPoints;
    }

    private List<TextSection> extractTextSectionsFallback(String rawData) {
        List<TextSection> sections = new ArrayList<>();
        String[] paragraphs = rawData.split("\n\n");

        for (int i = 0; i < Math.min(paragraphs.length, 10); i++) {
            String paragraph = paragraphs[i].trim();
            if (!paragraph.isEmpty() && paragraph.length() > 20) {
                TextSection section = TextSection.builder()
                        .content(paragraph)
                        .order(i)
                        .type(TextSection.SectionType.PARAGRAPH)
                        .build();
                sections.add(section);
            }
        }

        return sections;
    }

    private String extractTitleFallback(String rawData) {
        // Look for potential titles at the beginning
        String[] lines = rawData.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.length() > 10 && line.length() < 100 && !line.contains(".")) {
                return line;
            }
        }
        return "Data Analysis Report";
    }

    private String extractTitleFromResponse(String response) {
        Pattern titlePattern = Pattern.compile("(?i)title:\\s*(.+?)(?:\n|$)");
        Matcher matcher = titlePattern.matcher(response);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // Look for the first line that might be a title
        String[] lines = response.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.length() > 5 && line.length() < 100 && !line.contains(":")) {
                return line;
            }
        }

        return null;
    }

    private ExtractedData.DataType determineDataTypeFallback(List<DataPoint> dataPoints,
            List<TextSection> textSections) {
        if (dataPoints.isEmpty() && !textSections.isEmpty()) {
            return ExtractedData.DataType.TEXT_ONLY;
        } else if (!dataPoints.isEmpty() && textSections.isEmpty()) {
            return ExtractedData.DataType.NUMERICAL;
        } else if (!dataPoints.isEmpty() && !textSections.isEmpty()) {
            return ExtractedData.DataType.MIXED;
        } else {
            return ExtractedData.DataType.TEXT_ONLY;
        }
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null)
            return "";
        if (content.length() <= maxLength)
            return content;
        return content.substring(0, maxLength) + "...";
    }
}