package com.reportservice.infrastructure.adapter.out;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportservice.domain.model.DataPoint;
import com.reportservice.domain.model.ExtractedData;
import com.reportservice.domain.model.TextSection;
import com.reportservice.domain.port.out.DataExtractionPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataExtractionAdapter implements DataExtractionPort {
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public DataExtractionAdapter() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public CompletableFuture<String> fetchRawData(String url) {
        log.info("Fetching data from URL: {}", url);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("User-Agent", "Mozilla/5.0 (ReportGenerator/1.0)")
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body)
            .thenApply(body -> {
                log.info("Successfully fetched {} characters from {}", body.length(), url);
                return body;
            });
    }

    @Override
    public CompletableFuture<ExtractedData> extractAndAnalyzeData(String rawData, String sourceUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Try to parse as JSON first
                if (isJsonData(rawData)) {
                    return extractFromJson(rawData, sourceUrl);
                }
                // Try to parse as HTML
                else if (isHtmlData(rawData)) {
                    return extractFromHtml(rawData, sourceUrl);
                }
                // Try to parse as CSV
                else if (isCsvData(rawData)) {
                    return extractFromCsv(rawData, sourceUrl);
                }
                // Fall back to plain text extraction
                else {
                    return extractFromPlainText(rawData, sourceUrl);
                }
            } catch (IOException e) {
                log.error("Error extracting data from source: {}", sourceUrl, e);
                return createFallbackExtractedData(rawData, sourceUrl);
            }
        });
    }

    private boolean isJsonData(String data) {
        try {
            objectMapper.readTree(data);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean isHtmlData(String data) {
        return data.trim().startsWith("<") && data.contains("</");
    }

    private boolean isCsvData(String data) {
        String[] lines = data.split("\n");
        return lines.length > 1 && lines[0].contains(",");
    }

    private ExtractedData extractFromJson(String jsonData, String sourceUrl) throws IOException {
        JsonNode rootNode = objectMapper.readTree(jsonData);
        
        List<DataPoint> dataPoints = new ArrayList<>();
        List<TextSection> textSections = new ArrayList<>();
        Map<String, Object> metadata = new HashMap<>();
        
        // Extract numerical data
        extractNumericDataFromJson(rootNode, "", dataPoints);
        
        // Extract text content
        extractTextDataFromJson(rootNode, "", textSections);
        
        String title = extractTitle(rootNode);
        String summary = generateSummaryFromJson(rootNode);
        
        return ExtractedData.builder()
            .sourceUrl(sourceUrl)
            .title(title)
            .summary(summary)
            .dataType(determineDataType(dataPoints, textSections))
            .dataPoints(dataPoints)
            .textSections(textSections)
            .metadata(metadata)
            .build();
    }

    private ExtractedData extractFromHtml(String htmlData, String sourceUrl) {
        Document doc = Jsoup.parse(htmlData);
        
        List<DataPoint> dataPoints = new ArrayList<>();
        List<TextSection> textSections = new ArrayList<>();
        
        // Extract title
        String title = doc.title();
        if (title == null || title.isEmpty()) {
            Elements h1 = doc.select("h1");
            Element firstH1 = h1.first();
            title = (firstH1 == null || h1.isEmpty()) ? "Extracted Report" : firstH1.text();
        }
        
        // Extract tables for numerical data
        Elements tables = doc.select("table");
        for (Element table : tables) {
            extractDataFromTable(table, dataPoints);
        }
        
        // Extract text content
        Elements paragraphs = doc.select("p, h1, h2, h3, h4, h5, h6");
        int order = 0;
        for (Element element : paragraphs) {
            if (!element.text().trim().isEmpty()) {
                TextSection section = TextSection.builder()
                    .title(element.tagName().startsWith("h") ? element.text() : null)
                    .content(element.text())
                    .order(order++)
                    .type(determineTextSectionType(element.tagName()))
                    .build();
                textSections.add(section);
            }
        }
        
        // Extract numbers from text
        extractNumbersFromText(doc.text(), dataPoints);
        
        String summary = generateSummaryFromHtml(doc);
        
        return ExtractedData.builder()
            .sourceUrl(sourceUrl)
            .title(title)
            .summary(summary)
            .dataType(determineDataType(dataPoints, textSections))
            .dataPoints(dataPoints)
            .textSections(textSections)
            .metadata(Map.of("wordCount", doc.text().split("\\s+").length))
            .build();
    }

    private ExtractedData extractFromCsv(String csvData, String sourceUrl) {
        List<DataPoint> dataPoints = new ArrayList<>();
        List<TextSection> textSections = new ArrayList<>();
        
        String[] lines = csvData.split("\n");
        if (lines.length < 2) {
            return createFallbackExtractedData(csvData, sourceUrl);
        }
        
        String[] headers = lines[0].split(",");
        
        // Process CSV data
        for (int i = 1; i < lines.length; i++) {
            String[] values = lines[i].split(",");
            if (values.length >= 2) {
                try {
                    String label = values[0].trim();
                    Double value = Double.valueOf(values[1].trim());
                    String category = values.length > 2 ? values[2].trim() : "Default";
                    
                    DataPoint dataPoint = DataPoint.builder()
                        .label(label)
                        .value(value)
                        .category(category)
                        .build();
                    dataPoints.add(dataPoint);
                } catch (NumberFormatException e) {
                    // Skip non-numeric rows
                }
            }
        }
        
        return ExtractedData.builder()
            .sourceUrl(sourceUrl)
            .title("CSV Data Analysis")
            .summary(String.format("CSV data with %d records", dataPoints.size()))
            .dataType(ExtractedData.DataType.TABLE_DATA)
            .dataPoints(dataPoints)
            .textSections(textSections)
            .metadata(Map.of("rowCount", lines.length - 1, "columnCount", headers.length))
            .build();
    }

    private ExtractedData extractFromPlainText(String textData, String sourceUrl) {
        List<DataPoint> dataPoints = new ArrayList<>();
        List<TextSection> textSections = new ArrayList<>();
        
        // Extract numbers from text
        extractNumbersFromText(textData, dataPoints);
        
        // Split text into sections
        String[] paragraphs = textData.split("\n\n");
        int order = 0;
        for (String paragraph : paragraphs) {
            if (!paragraph.trim().isEmpty()) {
                TextSection section = TextSection.builder()
                    .content(paragraph.trim())
                    .order(order++)
                    .type(TextSection.SectionType.PARAGRAPH)
                    .build();
                textSections.add(section);
            }
        }
        
        return ExtractedData.builder()
            .sourceUrl(sourceUrl)
            .title("Text Analysis")
            .summary("Extracted data from plain text")
            .dataType(ExtractedData.DataType.TEXT_ONLY)
            .dataPoints(dataPoints)
            .textSections(textSections)
            .metadata(Map.of("characterCount", textData.length()))
            .build();
    }

    private void extractNumericDataFromJson(JsonNode node, String path, List<DataPoint> dataPoints) {
        if (node.isNumber()) {
            DataPoint dataPoint = DataPoint.builder()
                .label(path.isEmpty() ? "value" : path)
                .value(node.asDouble())
                .category("JSON")
                .build();
            dataPoints.add(dataPoint);
        } else if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String newPath = path.isEmpty() ? entry.getKey() : path + "." + entry.getKey();
                extractNumericDataFromJson(entry.getValue(), newPath, dataPoints);
            });
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                String newPath = path + "[" + i + "]";
                extractNumericDataFromJson(node.get(i), newPath, dataPoints);
            }
        }
    }

    private void extractTextDataFromJson(JsonNode node, String path, List<TextSection> textSections) {
        if (node.isTextual() && node.asText().length() > 10) {
            TextSection section = TextSection.builder()
                .title(path)
                .content(node.asText())
                .order(textSections.size())
                .type(TextSection.SectionType.PARAGRAPH)
                .build();
            textSections.add(section);
        } else if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String newPath = path.isEmpty() ? entry.getKey() : path + "." + entry.getKey();
                extractTextDataFromJson(entry.getValue(), newPath, textSections);
            });
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                String newPath = path + "[" + i + "]";
                extractTextDataFromJson(node.get(i), newPath, textSections);
            }
        }
    }

    private void extractDataFromTable(Element table, List<DataPoint> dataPoints) {
        Elements rows = table.select("tr");
        if (rows.size() < 2) return;
        
        Element headerRow = rows.first();
        if (headerRow == null) {
            return;
        }
        Elements headers = headerRow.select("th, td");
        
        for (int i = 1; i < rows.size(); i++) {
            Elements cells = rows.get(i).select("td");
            if (cells.size() >= 2) {
                try {
                    String label = cells.get(0).text().trim();
                    String valueText = cells.get(1).text().trim().replaceAll("[^0-9.-]", "");
                    Double value = Double.valueOf(valueText);
                    
                    DataPoint dataPoint = DataPoint.builder()
                        .label(label)
                        .value(value)
                        .category("Table")
                        .build();
                    dataPoints.add(dataPoint);
                } catch (NumberFormatException e) {
                    // Skip non-numeric data
                }
            }
        }
    }

    private void extractNumbersFromText(String text, List<DataPoint> dataPoints) {
        Pattern numberPattern = Pattern.compile("\\b\\d+(?:\\.\\d+)?\\b");
        Matcher matcher = numberPattern.matcher(text);
        
        int count = 0;
        while (matcher.find() && count < 50) { // Limit to prevent too many data points
            try {
                Double value = Double.valueOf(matcher.group());
                DataPoint dataPoint = DataPoint.builder()
                    .label("Number " + (count + 1))
                    .value(value)
                    .category("Text")
                    .build();
                dataPoints.add(dataPoint);
                count++;
            } catch (NumberFormatException e) {
                // Skip invalid numbers
            }
        }
    }

    private String extractTitle(JsonNode rootNode) {
        // Look for common title fields
        String[] titleFields = {"title", "name", "subject", "heading", "label"};
        for (String field : titleFields) {
            if (rootNode.has(field) && rootNode.get(field).isTextual()) {
                return rootNode.get(field).asText();
            }
        }
        return "JSON Data Analysis";
    }

    private String generateSummaryFromJson(JsonNode rootNode) {
        int objectCount = countObjects(rootNode);
        int arrayCount = countArrays(rootNode);
        int valueCount = countValues(rootNode);
        
        return String.format("JSON data containing %d objects, %d arrays, and %d values", 
            objectCount, arrayCount, valueCount);
    }

    private String generateSummaryFromHtml(Document doc) {
        int paragraphCount = doc.select("p").size();
        int tableCount = doc.select("table").size();
        int linkCount = doc.select("a").size();
        
        return String.format("HTML document with %d paragraphs, %d tables, and %d links", 
            paragraphCount, tableCount, linkCount);
    }

    private TextSection.SectionType determineTextSectionType(String tagName) {
        return switch (tagName.toLowerCase()) {
            case "h1", "h2", "h3", "h4", "h5", "h6" -> TextSection.SectionType.HEADER;
            case "blockquote" -> TextSection.SectionType.QUOTE;
            case "li" -> TextSection.SectionType.BULLET_POINT;
            default -> TextSection.SectionType.PARAGRAPH;
        };
    }

    private ExtractedData.DataType determineDataType(List<DataPoint> dataPoints, List<TextSection> textSections) {
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

    private ExtractedData createFallbackExtractedData(String rawData, String sourceUrl) {
        return ExtractedData.builder()
            .sourceUrl(sourceUrl)
            .title("Data Analysis")
            .summary("Basic data extraction performed")
            .dataType(ExtractedData.DataType.TEXT_ONLY)
            .textSections(List.of(TextSection.builder()
                .content(rawData.length() > 1000 ? rawData.substring(0, 1000) + "..." : rawData)
                .order(0)
                .type(TextSection.SectionType.PARAGRAPH)
                .build()))
            .metadata(Map.of("originalLength", rawData.length()))
            .build();
    }

    private int countObjects(JsonNode node) {
    if (node.isObject()) {
        int count = 1;
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            count += countObjects(entry.getValue());
        }
        return count;
    } else if (node.isArray()) {
        int count = 0;
        for (JsonNode child : node) {
            count += countObjects(child);
        }
        return count;
    }
    return 0;
}


    private int countArrays(JsonNode node) {
    if (node.isArray()) {
        int count = 1;
        for (JsonNode child : node) {
            count += countArrays(child);
        }
        return count;
    } else if (node.isObject()) {
        int count = 0;
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            count += countArrays(entry.getValue());
        }
        return count;
    }
    return 0;
}


    private int countValues(JsonNode node) {
    if (node.isValueNode()) {
        return 1;
    } else if (node.isObject()) {
        int count = 0;
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            count += countValues(entry.getValue());
        }
        return count;
    } else if (node.isArray()) {
        int count = 0;
        for (JsonNode child : node) {
            count += countValues(child);
        }
        return count;
    }
    return 0;
}

}