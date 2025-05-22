package com.reportservice.infrastructure.adapter.out;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.stereotype.Component;

import com.reportservice.domain.model.Chart;
import com.reportservice.domain.model.DataPoint;
import com.reportservice.domain.model.ExtractedData;
import com.reportservice.domain.port.out.ChartGenerationPort;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ChartGenerationAdapter implements ChartGenerationPort {

    private static final int CHART_WIDTH = 800;
    private static final int CHART_HEIGHT = 600;
    private static final Color[] CHART_COLORS = {
            new Color(31, 119, 180),
            new Color(255, 127, 14),
            new Color(44, 160, 44),
            new Color(214, 39, 40),
            new Color(148, 103, 189),
            new Color(140, 86, 75),
            new Color(227, 119, 194),
            new Color(127, 127, 127),
            new Color(188, 189, 34),
            new Color(23, 190, 207)
    };

    @Override
    public CompletableFuture<List<Chart>> generateCharts(ExtractedData data) {
        return CompletableFuture.supplyAsync(() -> {
            List<Chart> charts = new ArrayList<>();

            if (data.getDataPoints() == null || data.getDataPoints().isEmpty()) {
                log.info("No data points available for chart generation");
                return charts;
            }

            try {
                // Determine the best chart type based on data characteristics
                Chart.ChartType bestChartType = determineBestChartType(data);

                // Generate the primary chart
                Chart primaryChart = generateChart(bestChartType, data.getDataPoints(),
                        data.getTitle() != null ? data.getTitle() : "Data Analysis").join();
                charts.add(primaryChart);

                // Generate additional charts if data supports it
                if (data.getDataPoints().size() > 5) {
                    // Generate a secondary chart with different visualization
                    Chart.ChartType secondaryType = getSecondaryChartType(bestChartType);
                    Chart secondaryChart = generateChart(secondaryType, data.getDataPoints(),
                            "Alternative View - " + (data.getTitle() != null ? data.getTitle() : "Data")).join();
                    charts.add(secondaryChart);
                }

                // Generate category-based charts if categories are present
                if (hasCategoricalData(data.getDataPoints())) {
                    Chart categoryChart = generateCategoryChart(data.getDataPoints()).join();
                    charts.add(categoryChart);
                }

            } catch (Exception e) {
                log.error("Error generating charts", e);
                // Return empty list if chart generation fails
            }

            return charts;
        });
    }

    @Override
    public CompletableFuture<Chart> generateChart(Chart.ChartType type, List<DataPoint> dataPoints, String title) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JFreeChart jfreeChart = createJFreeChart(type, dataPoints, title);
                byte[] imageData = chartToByteArray(jfreeChart);

                return Chart.builder()
                        .title(title)
                        .type(type)
                        .dataPoints(dataPoints)
                        .imageData(imageData)
                        .description(generateChartDescription(type, dataPoints))
                        .xAxisLabel(determineXAxisLabel(dataPoints))
                        .yAxisLabel(determineYAxisLabel(dataPoints))
                        .build();

            } catch (IOException e) {
                log.error("Error generating {} chart: {}", type, e.getMessage(), e);
                throw new RuntimeException("Failed to generate chart", e);
            }
        });
    }

    @Override
    public byte[] chartToImage(Chart chart) {
        try {
            if (chart.getImageData() != null) {
                return chart.getImageData();
            }

            // Regenerate chart if image data is not available
            JFreeChart jfreeChart = createJFreeChart(chart.getType(), chart.getDataPoints(), chart.getTitle());
            return chartToByteArray(jfreeChart);

        } catch (IOException e) {
            log.error("Error converting chart to image", e);
            return new byte[0];
        }
    }

    private JFreeChart createJFreeChart(Chart.ChartType type, List<DataPoint> dataPoints, String title) {
        return switch (type) {
            case BAR -> createBarChart(dataPoints, title);
            case PIE -> createPieChart(dataPoints, title);
            case LINE -> createLineChart(dataPoints, title);
            case SCATTER -> createScatterChart(dataPoints, title);
            case HISTOGRAM -> createHistogramChart(dataPoints, title);
        };
    }

    private JFreeChart createBarChart(List<DataPoint> dataPoints, String title) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (DataPoint dataPoint : dataPoints) {
            String category = dataPoint.getCategory() != null ? dataPoint.getCategory() : "Data";
            String label = dataPoint.getLabel() != null ? dataPoint.getLabel() : "Value";
            Double valueObj = dataPoint.getValue();
            double value = valueObj != null ? valueObj : 0.0;
            dataset.addValue(value, category, label);

        }

        JFreeChart chart = ChartFactory.createBarChart(
                title,
                determineXAxisLabel(dataPoints),
                determineYAxisLabel(dataPoints),
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);

        customizeChart(chart);
        return chart;
    }

    private JFreeChart createPieChart(List<DataPoint> dataPoints, String title) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();

        // Limit to top 10 data points to avoid cluttered pie chart
        List<DataPoint> topDataPoints = dataPoints.stream()
                .sorted(Comparator.comparingDouble(
                        dp -> ((DataPoint) dp).getValue() != null ? ((DataPoint) dp).getValue() : 0.0).reversed())
                .limit(10)
                .collect(Collectors.toList());

        for (DataPoint dataPoint : topDataPoints) {
            String label = dataPoint.getLabel() != null ? dataPoint.getLabel() : "Unknown";
            Double valueObj = dataPoint.getValue();
            double value = valueObj != null ? valueObj.doubleValue() : 0.0;
            dataset.setValue(label, value);
        }

        JFreeChart chart = ChartFactory.createPieChart(
                title,
                dataset,
                true, // legend
                true, // tooltips
                false // urls
        );

        customizeChart(chart);
        return chart;
    }

    private JFreeChart createLineChart(List<DataPoint> dataPoints, String title) {
        XYSeriesCollection dataset = new XYSeriesCollection();

        // Group data points by category
        Map<String, List<DataPoint>> categorizedData = dataPoints.stream()
                .collect(Collectors.groupingBy(dp -> dp.getCategory() != null ? dp.getCategory() : "Default"));

        for (Map.Entry<String, List<DataPoint>> entry : categorizedData.entrySet()) {
            XYSeries series = new XYSeries(entry.getKey());

            List<DataPoint> sortedPoints = entry.getValue().stream()
                    .sorted((a, b) -> {
                        if (a.getDate() != null && b.getDate() != null) {
                            return a.getDate().compareTo(b.getDate());
                        }
                        return 0;
                    })
                    .collect(Collectors.toList());

            for (int i = 0; i < sortedPoints.size(); i++) {
                DataPoint dp = sortedPoints.get(i);
                Double value = dp.getValue() != null ? dp.getValue() : 0.0;
                series.add(i, value);
            }

            dataset.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
                title,
                determineXAxisLabel(dataPoints),
                determineYAxisLabel(dataPoints),
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);

        customizeChart(chart);
        return chart;
    }

    private JFreeChart createScatterChart(List<DataPoint> dataPoints, String title) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series = new XYSeries("Data Points");

        for (int i = 0; i < dataPoints.size(); i++) {
            DataPoint dp = dataPoints.get(i);
            Double value = dp.getValue() != null ? dp.getValue() : 0.0;
            series.add(i, value);
        }

        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createScatterPlot(
                title,
                determineXAxisLabel(dataPoints),
                determineYAxisLabel(dataPoints),
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);

        customizeChart(chart);
        return chart;
    }

    private JFreeChart createHistogramChart(List<DataPoint> dataPoints, String title) {
        double[] values = dataPoints.stream()
                .mapToDouble(dp -> dp.getValue() != null ? dp.getValue() : 0.0)
                .toArray();

        HistogramDataset dataset = new HistogramDataset();
        dataset.addSeries("Frequency", values, Math.min(20, values.length / 2));

        JFreeChart chart = ChartFactory.createHistogram(
                title,
                determineXAxisLabel(dataPoints),
                "Frequency",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);

        customizeChart(chart);
        return chart;
    }

    private void customizeChart(JFreeChart chart) {
        // Set background color
        chart.setBackgroundPaint(Color.WHITE);

        // Customize title
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 16));
        chart.getTitle().setPaint(Color.BLACK);

        // Customize plot
        chart.getPlot().setBackgroundPaint(Color.WHITE);
        chart.getPlot().setOutlineStroke(new BasicStroke(1.0f));
        chart.getPlot().setOutlinePaint(Color.GRAY);

        // Add subtle grid lines if applicable
        if (chart.getPlot() instanceof org.jfree.chart.plot.CategoryPlot categoryPlot) {
            categoryPlot.setDomainGridlinesVisible(true);
            categoryPlot.setRangeGridlinesVisible(true);
            categoryPlot.setDomainGridlinePaint(Color.LIGHT_GRAY);
            categoryPlot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        } else if (chart.getPlot() instanceof org.jfree.chart.plot.XYPlot xyPlot) {
            xyPlot.setDomainGridlinesVisible(true);
            xyPlot.setRangeGridlinesVisible(true);
            xyPlot.setDomainGridlinePaint(Color.LIGHT_GRAY);
            xyPlot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        }
    }

    private byte[] chartToByteArray(JFreeChart chart) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(baos, chart, CHART_WIDTH, CHART_HEIGHT);
        return baos.toByteArray();
    }

    private Chart.ChartType determineBestChartType(ExtractedData data) {
        List<DataPoint> dataPoints = data.getDataPoints();

        if (dataPoints.size() <= 5 && hasCategoricalData(dataPoints)) {
            return Chart.ChartType.PIE;
        } else if (hasTimeSeriesData(dataPoints)) {
            return Chart.ChartType.LINE;
        } else if (dataPoints.size() > 20) {
            return Chart.ChartType.HISTOGRAM;
        } else if (hasCategoricalData(dataPoints)) {
            return Chart.ChartType.BAR;
        } else {
            return Chart.ChartType.BAR;
        }
    }

    private Chart.ChartType getSecondaryChartType(Chart.ChartType primaryType) {
        return switch (primaryType) {
            case BAR -> Chart.ChartType.PIE;
            case PIE -> Chart.ChartType.BAR;
            case LINE -> Chart.ChartType.SCATTER;
            case SCATTER -> Chart.ChartType.LINE;
            case HISTOGRAM -> Chart.ChartType.BAR;
        };
    }

    private CompletableFuture<Chart> generateCategoryChart(List<DataPoint> dataPoints) {
        return CompletableFuture.supplyAsync(() -> {
            // Group by category and sum values
            Map<String, Double> categoryTotals = dataPoints.stream()
                    .filter(dp -> dp.getCategory() != null && dp.getValue() != null)
                    .collect(Collectors.groupingBy(
                            DataPoint::getCategory,
                            Collectors.summingDouble(DataPoint::getValue)));

            List<DataPoint> categoryDataPoints = categoryTotals.entrySet().stream()
                    .map(entry -> DataPoint.builder()
                            .label(entry.getKey())
                            .value(entry.getValue())
                            .category("Summary")
                            .build())
                    .collect(Collectors.toList());

            return generateChart(Chart.ChartType.PIE, categoryDataPoints, "Category Summary").join();
        });
    }

    private boolean hasCategoricalData(List<DataPoint> dataPoints) {
        return dataPoints.stream().anyMatch(dp -> dp.getCategory() != null && !dp.getCategory().trim().isEmpty());
    }

    private boolean hasTimeSeriesData(List<DataPoint> dataPoints) {
        return dataPoints.stream().anyMatch(dp -> dp.getDate() != null);
    }

    private String determineXAxisLabel(List<DataPoint> dataPoints) {
        if (hasTimeSeriesData(dataPoints)) {
            return "Time";
        } else if (hasCategoricalData(dataPoints)) {
            return "Category";
        } else {
            return "Items";
        }
    }

    private String determineYAxisLabel(List<DataPoint> dataPoints) {
        // Check if all data points have the same unit
        String commonUnit = dataPoints.stream()
                .map(DataPoint::getUnit)
                .filter(unit -> unit != null && !unit.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList())
                .stream()
                .findFirst()
                .orElse(null);

        if (commonUnit != null) {
            return "Value (" + commonUnit + ")";
        } else {
            return "Value";
        }
    }

    private String generateChartDescription(Chart.ChartType type, List<DataPoint> dataPoints) {
        int dataPointCount = dataPoints.size();
        double maxValue = dataPoints.stream()
                .mapToDouble(dp -> dp.getValue() != null ? dp.getValue() : 0.0)
                .max()
                .orElse(0.0);
        double minValue = dataPoints.stream()
                .mapToDouble(dp -> dp.getValue() != null ? dp.getValue() : 0.0)
                .min()
                .orElse(0.0);

        return String.format("%s chart displaying %d data points with values ranging from %.2f to %.2f",
                type.name().toLowerCase(), dataPointCount, minValue, maxValue);
    }
}