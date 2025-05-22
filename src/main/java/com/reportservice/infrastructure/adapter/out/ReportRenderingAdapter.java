package com.reportservice.infrastructure.adapter.out;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.reportservice.domain.model.*;
import com.reportservice.domain.port.out.ReportRenderingPort;

import lombok.extern.slf4j.Slf4j;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

@Slf4j
@Component
public class ReportRenderingAdapter implements ReportRenderingPort {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public CompletableFuture<GeneratedReport> renderToPdf(ReportContent content, ReportRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PdfWriter writer = new PdfWriter(baos);
                PdfDocument pdfDoc = new PdfDocument(writer);
                // Add title
                try (Document document = new Document(pdfDoc)) {
                    // Add title
                    if (content.getTitle() != null) {
                        Paragraph title = new Paragraph(content.getTitle())
                                .setFontSize(20)
                                .setBold()
                                .setTextAlignment(TextAlignment.CENTER)
                                .setMarginBottom(20);
                        document.add(title);
                    }
                    
                    // Add metadata
                    addMetadataToPdf(document, content);
                    
                    // Add summary
                    if (content.getSummary() != null) {
                        Paragraph summaryTitle = new Paragraph("Executive Summary")
                                .setFontSize(16)
                                .setBold()
                                .setMarginTop(20)
                                .setMarginBottom(10);
                        document.add(summaryTitle);
                        
                        Paragraph summary = new Paragraph(content.getSummary())
                                .setFontSize(12)
                                .setMarginBottom(15);
                        document.add(summary);
                    }
                    
                    // Add text sections
                    addTextSectionsToPdf(document, content.getSections());
                    
                    // Add charts
                    addChartsToPdf(document, content.getCharts());
                    
                    // Add tables
                    addTablesToPdf(document, content.getTables());
                }

                String fileName = generateFileName(request, "pdf");
                return GeneratedReport.builder()
                    .id(UUID.randomUUID().toString())
                    .fileName(fileName)
                    .format(ReportRequest.ReportFormat.PDF)
                    .content(baos.toByteArray())
                    .mimeType("application/pdf")
                    .sizeInBytes(baos.size())
                    .generatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build();

            } catch (Exception e) {
                log.error("Error generating PDF report", e);
                throw new RuntimeException("Failed to generate PDF report", e);
            }
        });
    }

    @Override
    public CompletableFuture<GeneratedReport> renderToDocx(ReportContent content, ReportRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ByteArrayOutputStream baos;
                // Add title
                try (XWPFDocument document = new XWPFDocument()) {
                    // Add title
                    if (content.getTitle() != null) {
                        XWPFParagraph titlePara = document.createParagraph();
                        titlePara.setAlignment(ParagraphAlignment.CENTER);
                        XWPFRun titleRun = titlePara.createRun();
                        titleRun.setText(content.getTitle());
                        titleRun.setBold(true);
                        titleRun.setFontSize(20);
                        titleRun.addBreak();
                        titleRun.addBreak();
                    }   // Add metadata
                    addMetadataToDocx(document, content);
                    // Add summary
                    if (content.getSummary() != null) {
                        XWPFParagraph summaryTitle = document.createParagraph();
                        XWPFRun summaryTitleRun = summaryTitle.createRun();
                        summaryTitleRun.setText("Executive Summary");
                        summaryTitleRun.setBold(true);
                        summaryTitleRun.setFontSize(16);
                        summaryTitleRun.addBreak();
                        
                        XWPFParagraph summaryPara = document.createParagraph();
                        XWPFRun summaryRun = summaryPara.createRun();
                        summaryRun.setText(content.getSummary());
                        summaryRun.setFontSize(12);
                        summaryRun.addBreak();
                        summaryRun.addBreak();
                    }   // Add text sections
                    addTextSectionsToDocx(document, content.getSections());
                    // Add charts
                    addChartsToDocx(document, content.getCharts());
                    // Add tables
                    addTablesToDocx(document, content.getTables());
                    baos = new ByteArrayOutputStream();
                    document.write(baos);
                }

                String fileName = generateFileName(request, "docx");
                return GeneratedReport.builder()
                    .id(UUID.randomUUID().toString())
                    .fileName(fileName)
                    .format(ReportRequest.ReportFormat.DOCX)
                    .content(baos.toByteArray())
                    .mimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    .sizeInBytes(baos.size())
                    .generatedAt(OffsetDateTime.now(ZoneOffset.UTC)
)
                    .build();

            } catch (IOException e) {
                log.error("Error generating DOCX report", e);
                throw new RuntimeException("Failed to generate DOCX report", e);
            }
        });
    }

    @Override
    public CompletableFuture<GeneratedReport> renderToLatexPdf(ReportContent content, ReportRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Generate LaTeX content
                String latexContent = generateLatexContent(content);
                
                // For this implementation, we'll convert the LaTeX to PDF using iText
                // In a production environment, you might want to use a proper LaTeX processor
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PdfWriter writer = new PdfWriter(baos);
                PdfDocument pdfDoc = new PdfDocument(writer);
                // Add LaTeX-style formatting
                try (Document document = new Document(pdfDoc)) {
                    // Add LaTeX-style formatting
                    addLatexStyledContent(document, content);
                }

                String fileName = generateFileName(request, "pdf");
                return GeneratedReport.builder()
                    .id(UUID.randomUUID().toString())
                    .fileName(fileName.replace(".pdf", "_latex.pdf"))
                    .format(ReportRequest.ReportFormat.PDF)
                    .content(baos.toByteArray())
                    .mimeType("application/pdf")
                    .sizeInBytes(baos.size())
                    .generatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build();

            } catch (Exception e) {
                log.error("Error generating LaTeX PDF report", e);
                throw new RuntimeException("Failed to generate LaTeX PDF report", e);
            }
        });
    }

    private void addMetadataToPdf(Document document, ReportContent content) {
        Table metadataTable = new Table(2);
        metadataTable.setWidth(UnitValue.createPercentValue(100));

        metadataTable.addCell(new Cell().add(new Paragraph("Generated At:").setBold()));
        metadataTable.addCell(new Cell().add(new Paragraph(content.getGeneratedAt().format(DATE_FORMATTER))));

        metadataTable.addCell(new Cell().add(new Paragraph("Source URL:").setBold()));
        metadataTable.addCell(new Cell().add(new Paragraph(content.getSourceUrl())));

        document.add(metadataTable);
        document.add(new Paragraph().setMarginBottom(15));
    }

    private void addMetadataToDocx(XWPFDocument document, ReportContent content) {
        XWPFTable metadataTable = document.createTable(2, 2);
        
        metadataTable.getRow(0).getCell(0).setText("Generated At:");
        metadataTable.getRow(0).getCell(1).setText(content.getGeneratedAt().format(DATE_FORMATTER));
        
        metadataTable.getRow(1).getCell(0).setText("Source URL:");
        metadataTable.getRow(1).getCell(1).setText(content.getSourceUrl());

        // Style the table
        for (XWPFTableRow row : metadataTable.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                XWPFParagraph para = cell.getParagraphArray(0);
                if (para != null) {
                    XWPFRun run = para.getRuns().isEmpty() ? para.createRun() : para.getRuns().get(0);
                    run.setFontSize(10);
                }
            }
        }
    }

    private void addTextSectionsToPdf(Document document, java.util.List<TextSection> sections) {
        if (sections == null || sections.isEmpty()) return;

        for (TextSection section : sections) {
            if (section.getTitle() != null) {
                Paragraph sectionTitle = new Paragraph(section.getTitle())
                    .setFontSize(14)
                    .setBold()
                    .setMarginTop(15)
                    .setMarginBottom(5);
                document.add(sectionTitle);
            }

            Paragraph sectionContent = new Paragraph(section.getContent())
                .setFontSize(11)
                .setMarginBottom(10);
            document.add(sectionContent);
        }
    }

    private void addTextSectionsToDocx(XWPFDocument document, java.util.List<TextSection> sections) {
        if (sections == null || sections.isEmpty()) return;

        for (TextSection section : sections) {
            if (section.getTitle() != null) {
                XWPFParagraph titlePara = document.createParagraph();
                XWPFRun titleRun = titlePara.createRun();
                titleRun.setText(section.getTitle());
                titleRun.setBold(true);
                titleRun.setFontSize(14);
                titleRun.addBreak();
            }

            XWPFParagraph contentPara = document.createParagraph();
            XWPFRun contentRun = contentPara.createRun();
            contentRun.setText(section.getContent());
            contentRun.setFontSize(11);
            contentRun.addBreak();
        }
    }

    private void addChartsToPdf(Document document, java.util.List<Chart> charts) {
        if (charts == null || charts.isEmpty()) return;

        Paragraph chartsTitle = new Paragraph("Charts and Visualizations")
            .setFontSize(16)
            .setBold()
            .setMarginTop(20)
            .setMarginBottom(10);
        document.add(chartsTitle);

        for (Chart chart : charts) {
            if (chart.getTitle() != null) {
                Paragraph chartTitle = new Paragraph(chart.getTitle())
                    .setFontSize(12)
                    .setBold()
                    .setMarginTop(15)
                    .setMarginBottom(5);
                document.add(chartTitle);
            }

            if (chart.getImageData() != null && chart.getImageData().length > 0) {
                try {
                    Image chartImage = new Image(ImageDataFactory.create(chart.getImageData()));
                    chartImage.setWidth(UnitValue.createPercentValue(80));
                    chartImage.setAutoScale(true);
                    document.add(chartImage);
                } catch (Exception e) {
                    log.warn("Failed to add chart image to PDF", e);
                    Paragraph errorMsg = new Paragraph("Chart image could not be displayed")
                        .setFontSize(10)
                        .setItalic();
                    document.add(errorMsg);
                }
            }

            if (chart.getDescription() != null) {
                Paragraph description = new Paragraph(chart.getDescription())
                    .setFontSize(10)
                    .setItalic()
                    .setMarginBottom(10);
                document.add(description);
            }
        }
    }

    private void addChartsToDocx(XWPFDocument document, java.util.List<Chart> charts) {
        if (charts == null || charts.isEmpty()) return;

        XWPFParagraph chartsTitle = document.createParagraph();
        XWPFRun chartsTitleRun = chartsTitle.createRun();
        chartsTitleRun.setText("Charts and Visualizations");
        chartsTitleRun.setBold(true);
        chartsTitleRun.setFontSize(16);
        chartsTitleRun.addBreak();

        for (Chart chart : charts) {
            if (chart.getTitle() != null) {
                XWPFParagraph chartTitle = document.createParagraph();
                XWPFRun chartTitleRun = chartTitle.createRun();
                chartTitleRun.setText(chart.getTitle());
                chartTitleRun.setBold(true);
                chartTitleRun.setFontSize(12);
                chartTitleRun.addBreak();
            }

            if (chart.getImageData() != null && chart.getImageData().length > 0) {
                try {
                    XWPFParagraph imagePara = document.createParagraph();
                    XWPFRun imageRun = imagePara.createRun();
                    
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(chart.getImageData())) {
                        imageRun.addPicture(bis, XWPFDocument.PICTURE_TYPE_PNG, "chart.png",
                                Units.toEMU(400), Units.toEMU(300));
                    }
                } catch (IOException | InvalidFormatException e) {
                    log.warn("Failed to add chart image to DOCX", e);
                    XWPFParagraph errorPara = document.createParagraph();
                    XWPFRun errorRun = errorPara.createRun();
                    errorRun.setText("Chart image could not be displayed");
                    errorRun.setItalic(true);
                    errorRun.setFontSize(10);
                }
            }

            if (chart.getDescription() != null) {
                XWPFParagraph descPara = document.createParagraph();
                XWPFRun descRun = descPara.createRun();
                descRun.setText(chart.getDescription());
                descRun.setItalic(true);
                descRun.setFontSize(10);
                descRun.addBreak();
            }
        }
    }

    private void addTablesToPdf(Document document, java.util.List<com.reportservice.domain.model.Table> tables) {
        if (tables == null || tables.isEmpty()) return;

        Paragraph tablesTitle = new Paragraph("Data Tables")
            .setFontSize(16)
            .setBold()
            .setMarginTop(20)
            .setMarginBottom(10);
        document.add(tablesTitle);

        for (com.reportservice.domain.model.Table table : tables) {
            if (table.getTitle() != null) {
                Paragraph tableTitle = new Paragraph(table.getTitle())
                    .setFontSize(12)
                    .setBold()
                    .setMarginTop(15)
                    .setMarginBottom(5);
                document.add(tableTitle);
            }

            if (table.getHeaders() != null && !table.getHeaders().isEmpty()) {
                Table pdfTable = new Table(table.getHeaders().size());
                pdfTable.setWidth(UnitValue.createPercentValue(100));

                // Add headers
                for (String header : table.getHeaders()) {
                    Cell headerCell = new Cell().add(new Paragraph(header).setBold());
                    headerCell.setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY);
                    pdfTable.addHeaderCell(headerCell);
                }

                // Add rows
                if (table.getRows() != null) {
                    for (java.util.List<String> row : table.getRows()) {
                        for (String cellValue : row) {
                            pdfTable.addCell(new Cell().add(new Paragraph(cellValue != null ? cellValue : "")));
                        }
                    }
                }

                document.add(pdfTable);
            }

            if (table.getDescription() != null) {
                Paragraph description = new Paragraph(table.getDescription())
                    .setFontSize(10)
                    .setItalic()
                    .setMarginBottom(15);
                document.add(description);
            }
        }
    }

    private void addTablesToDocx(XWPFDocument document, java.util.List<com.reportservice.domain.model.Table> tables) {
        if (tables == null || tables.isEmpty()) return;

        XWPFParagraph tablesTitle = document.createParagraph();
        XWPFRun tablesTitleRun = tablesTitle.createRun();
        tablesTitleRun.setText("Data Tables");
        tablesTitleRun.setBold(true);
        tablesTitleRun.setFontSize(16);
        tablesTitleRun.addBreak();

        for (com.reportservice.domain.model.Table table : tables) {
            if (table.getTitle() != null) {
                XWPFParagraph tableTitle = document.createParagraph();
                XWPFRun tableTitleRun = tableTitle.createRun();
                tableTitleRun.setText(table.getTitle());
                tableTitleRun.setBold(true);
                tableTitleRun.setFontSize(12);
                tableTitleRun.addBreak();
            }

            if (table.getHeaders() != null && !table.getHeaders().isEmpty()) {
                int rows = 1 + (table.getRows() != null ? table.getRows().size() : 0);
                XWPFTable docxTable = document.createTable(rows, table.getHeaders().size());

                // Add headers
                XWPFTableRow headerRow = docxTable.getRow(0);
                for (int i = 0; i < table.getHeaders().size(); i++) {
                    XWPFTableCell cell = headerRow.getCell(i);
                    cell.setText(table.getHeaders().get(i));
                    // Style header cells
                    cell.setColor("CCCCCC");
                    XWPFParagraph para = cell.getParagraphArray(0);
                    if (para != null && !para.getRuns().isEmpty()) {
                        para.getRuns().get(0).setBold(true);
                    }
                }

                // Add data rows
                if (table.getRows() != null) {
                    for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
                        XWPFTableRow dataRow = docxTable.getRow(rowIndex + 1);
                        java.util.List<String> rowData = table.getRows().get(rowIndex);
                        
                        for (int colIndex = 0; colIndex < rowData.size() && colIndex < table.getHeaders().size(); colIndex++) {
                            XWPFTableCell cell = dataRow.getCell(colIndex);
                            cell.setText(rowData.get(colIndex) != null ? rowData.get(colIndex) : "");
                        }
                    }
                }
            }

            if (table.getDescription() != null) {
                XWPFParagraph descPara = document.createParagraph();
                XWPFRun descRun = descPara.createRun();
                descRun.setText(table.getDescription());
                descRun.setItalic(true);
                descRun.setFontSize(10);
                descRun.addBreak();
            }
        }
    }

    private String generateLatexContent(ReportContent content) {
        StringBuilder latex = new StringBuilder();
        
        latex.append("\\documentclass{article}\n");
        latex.append("\\usepackage[utf8]{inputenc}\n");
        latex.append("\\usepackage{graphicx}\n");
        latex.append("\\usepackage{booktabs}\n");
        latex.append("\\usepackage{geometry}\n");
        latex.append("\\geometry{margin=1in}\n");
        latex.append("\\title{").append(escapeLatex(content.getTitle())).append("}\n");
        latex.append("\\date{").append(content.getGeneratedAt().format(DATE_FORMATTER)).append("}\n");
        latex.append("\\begin{document}\n");
        latex.append("\\maketitle\n\n");

        // Add summary
        if (content.getSummary() != null) {
            latex.append("\\section{Executive Summary}\n");
            latex.append(escapeLatex(content.getSummary())).append("\n\n");
        }

        // Add text sections
        if (content.getSections() != null) {
            for (TextSection section : content.getSections()) {
                if (section.getTitle() != null) {
                    latex.append("\\subsection{").append(escapeLatex(section.getTitle())).append("}\n");
                }
                latex.append(escapeLatex(section.getContent())).append("\n\n");
            }
        }

        // Add tables
        if (content.getTables() != null) {
            latex.append("\\section{Data Tables}\n");
            for (com.reportservice.domain.model.Table table : content.getTables()) {
                if (table.getTitle() != null) {
                    latex.append("\\subsection{").append(escapeLatex(table.getTitle())).append("}\n");
                }
                
                if (table.getHeaders() != null && !table.getHeaders().isEmpty()) {
                    latex.append("\\begin{table}[h!]\n");
                    latex.append("\\centering\n");
                    latex.append("\\begin{tabular}{");
                    for (int i = 0; i < table.getHeaders().size(); i++) {
                        latex.append("l");
                    }
                    latex.append("}\n");
                    latex.append("\\toprule\n");
                    
                    // Headers
                    for (int i = 0; i < table.getHeaders().size(); i++) {
                        latex.append(escapeLatex(table.getHeaders().get(i)));
                        if (i < table.getHeaders().size() - 1) {
                            latex.append(" & ");
                        }
                    }
                    latex.append(" \\\\\n");
                    latex.append("\\midrule\n");
                    
                    // Rows
                    if (table.getRows() != null) {
                        for (java.util.List<String> row : table.getRows()) {
                            for (int i = 0; i < row.size() && i < table.getHeaders().size(); i++) {
                                latex.append(escapeLatex(row.get(i) != null ? row.get(i) : ""));
                                if (i < Math.min(row.size(), table.getHeaders().size()) - 1) {
                                    latex.append(" & ");
                                }
                            }
                            latex.append(" \\\\\n");
                        }
                    }
                    
                    latex.append("\\bottomrule\n");
                    latex.append("\\end{tabular}\n");
                    latex.append("\\end{table}\n\n");
                }
            }
        }

        latex.append("\\end{document}\n");
        return latex.toString();
    }

    private void addLatexStyledContent(Document document, ReportContent content) {
        // This method adds LaTeX-style formatting to the PDF using iText
        // Title with LaTeX-style formatting
        if (content.getTitle() != null) {
            Paragraph title = new Paragraph(content.getTitle())
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30);
            document.add(title);
        }

        // Date in LaTeX style
        Paragraph date = new Paragraph("Generated: " + content.getGeneratedAt().format(DATE_FORMATTER))
            .setFontSize(10)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20);
        document.add(date);

        // Add content with LaTeX-style section numbering
        int sectionNumber = 1;
        
        if (content.getSummary() != null) {
            Paragraph sectionTitle = new Paragraph(sectionNumber + ". Executive Summary")
                .setFontSize(14)
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(10);
            document.add(sectionTitle);
            
            Paragraph summary = new Paragraph(content.getSummary())
                .setFontSize(11)
                .setMarginBottom(15);
            document.add(summary);
            sectionNumber++;
        }

        // Add other sections with LaTeX styling
        addTextSectionsToPdf(document, content.getSections());
        addChartsToPdf(document, content.getCharts());
        addTablesToPdf(document, content.getTables());
    }

    private String escapeLatex(String text) {
        if (text == null) return "";
        
        return text.replace("\\", "\\textbackslash ")
                  .replace("{", "\\{")
                  .replace("}", "\\}")
                  .replace("$", "\\$")
                  .replace("&", "\\&")
                  .replace("%", "\\%")
                  .replace("#", "\\#")
                  .replace("^", "\\textasciicircum ")
                  .replace("_", "\\_")
                  .replace("~", "\\textasciitilde ");
    }

    private String generateFileName(ReportRequest request, String extension) {
        String baseFileName = "report";
        
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            baseFileName = request.getTitle().trim()
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .toLowerCase();
        }
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("%s_%s.%s", baseFileName, timestamp, extension);
    }
}