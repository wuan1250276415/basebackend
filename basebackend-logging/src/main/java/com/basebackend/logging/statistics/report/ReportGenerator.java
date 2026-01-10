package com.basebackend.logging.statistics.report;

import com.basebackend.logging.statistics.aggregator.StatisticsAggregator;
import com.basebackend.logging.statistics.analyzer.PatternAnalyzer;
import com.basebackend.logging.statistics.model.LogStatisticsEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计报告生成器
 *
 * 提供多格式统计报告生成功能：
 * - JSON 格式报告
 * - HTML 格式报告
 * - PDF 格式报告（待实现）
 * - Excel 格式报告（待实现）
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
public class ReportGenerator {

    private final ObjectMapper objectMapper;
    private final StatisticsAggregator aggregator;
    private final PatternAnalyzer patternAnalyzer;

    public ReportGenerator(ObjectMapper objectMapper,
            StatisticsAggregator aggregator,
            PatternAnalyzer patternAnalyzer) {
        this.objectMapper = objectMapper;
        this.aggregator = aggregator;
        this.patternAnalyzer = patternAnalyzer;
    }

    /**
     * 生成 JSON 格式报告
     *
     * @param data         统计数据
     * @param reportConfig 报告配置
     * @return 报告内容
     */
    public ReportResult generateJsonReport(
            List<LogStatisticsEntry> data,
            ReportConfig reportConfig) {
        if (data == null || data.isEmpty()) {
            return ReportResult.builder()
                    .format("JSON")
                    .success(false)
                    .error("数据为空")
                    .build();
        }

        try {
            Map<String, Object> reportData = buildReportData(data, reportConfig);
            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(reportData);

            String fileName = generateFileName(reportConfig, "json");
            saveToFile(fileName, jsonContent);

            log.info("JSON 报告生成成功: {}", fileName);

            return ReportResult.builder()
                    .format("JSON")
                    .success(true)
                    .fileName(fileName)
                    .content(jsonContent)
                    .build();

        } catch (Exception e) {
            log.error("JSON 报告生成失败", e);
            return ReportResult.builder()
                    .format("JSON")
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * 生成 HTML 格式报告
     *
     * @param data         统计数据
     * @param reportConfig 报告配置
     * @return 报告内容
     */
    public ReportResult generateHtmlReport(
            List<LogStatisticsEntry> data,
            ReportConfig reportConfig) {
        if (data == null || data.isEmpty()) {
            return ReportResult.builder()
                    .format("HTML")
                    .success(false)
                    .error("数据为空")
                    .build();
        }

        try {
            Map<String, Object> reportData = buildReportData(data, reportConfig);
            String htmlContent = buildHtmlContent(reportData, reportConfig);

            String fileName = generateFileName(reportConfig, "html");
            saveToFile(fileName, htmlContent);

            log.info("HTML 报告生成成功: {}", fileName);

            return ReportResult.builder()
                    .format("HTML")
                    .success(true)
                    .fileName(fileName)
                    .content(htmlContent)
                    .build();

        } catch (Exception e) {
            log.error("HTML 报告生成失败", e);
            return ReportResult.builder()
                    .format("HTML")
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * 生成多格式报告
     *
     * @param data         统计数据
     * @param reportConfig 报告配置
     * @return 多格式报告结果
     */
    public MultiFormatReportResult generateMultiFormatReports(
            List<LogStatisticsEntry> data,
            ReportConfig reportConfig) {
        if (data == null || data.isEmpty()) {
            return MultiFormatReportResult.builder()
                    .success(false)
                    .error("数据为空")
                    .build();
        }

        Map<String, ReportResult> reports = new HashMap<>();

        // 生成 JSON 报告
        if (reportConfig.isEnableJson()) {
            reports.put("JSON", generateJsonReport(data, reportConfig));
        }

        // 生成 HTML 报告
        if (reportConfig.isEnableHtml()) {
            reports.put("HTML", generateHtmlReport(data, reportConfig));
        }

        // 生成 PDF 报告 (TODO: 未来实现)
        if (reportConfig.isEnablePdf()) {
            // reports.put("PDF", generatePdfReport(data, reportConfig));
        }

        // 生成 Excel 报告 (TODO: 未来实现)
        if (reportConfig.isEnableExcel()) {
            // reports.put("Excel", generateExcelReport(data, reportConfig));
        }

        boolean allSuccess = reports.values().stream()
                .map(ReportResult::isSuccess)
                .reduce(true, Boolean::logicalAnd);

        log.info("多格式报告生成完成: {} 种格式, success={}",
                reports.size(), allSuccess);

        return MultiFormatReportResult.builder()
                .success(allSuccess)
                .reports(reports)
                .totalFormats(reports.size())
                .successfulFormats((int) reports.values().stream()
                        .filter(ReportResult::isSuccess)
                        .count())
                .build();
    }

    /**
     * 生成报告摘要
     *
     * @param data 统计数据
     * @return 报告摘要
     */
    public ReportSummary generateSummary(List<LogStatisticsEntry> data) {
        if (data == null || data.isEmpty()) {
            return ReportSummary.builder()
                    .totalEntries(0)
                    .build();
        }

        double totalCount = data.stream()
                .mapToDouble(LogStatisticsEntry::getCount)
                .sum();

        double averageCount = totalCount / data.size();

        double maxCount = data.stream()
                .mapToDouble(LogStatisticsEntry::getCount)
                .max()
                .orElse(0.0);

        double minCount = data.stream()
                .mapToDouble(LogStatisticsEntry::getCount)
                .min()
                .orElse(0.0);

        Instant startTime = data.stream()
                .map(LogStatisticsEntry::getStartTime)
                .min(Instant::compareTo)
                .orElse(Instant.now());

        Instant endTime = data.stream()
                .map(LogStatisticsEntry::getEndTime)
                .max(Instant::compareTo)
                .orElse(Instant.now());

        // 识别主要趋势
        String mainTrend = identifyMainTrend(data);

        // 检测异常值
        long anomalyCount = data.stream()
                .mapToLong(LogStatisticsEntry::getAnomalyCount)
                .sum();

        log.debug("报告摘要生成完成: entries={}, totalCount={}, trend={}",
                data.size(), totalCount, mainTrend);

        return ReportSummary.builder()
                .totalEntries(data.size())
                .totalCount(totalCount)
                .averageCount(averageCount)
                .maxCount(maxCount)
                .minCount(minCount)
                .startTime(startTime)
                .endTime(endTime)
                .duration(java.time.Duration.between(startTime, endTime))
                .mainTrend(mainTrend)
                .anomalyCount(anomalyCount)
                .build();
    }

    // ==================== 私有辅助方法 ====================

    private Map<String, Object> buildReportData(
            List<LogStatisticsEntry> data,
            ReportConfig config) {
        Map<String, Object> reportData = new HashMap<>();

        // 基本信息
        reportData.put("reportTitle", config.getTitle());
        reportData.put("generatedAt", LocalDateTime.now().format(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        reportData.put("dataRange", getDataRange(data));

        // 摘要信息
        reportData.put("summary", generateSummary(data));

        // 详细统计数据
        reportData.put("statistics", data);

        // 聚合分析
        if (config.isIncludeAggregations()) {
            reportData.put("aggregations", generateAggregations(data));
        }

        // Top-N 分析
        if (config.isIncludeTopN()) {
            reportData.put("topN", generateTopNAnalysis(data));
        }

        // 趋势分析
        if (config.isIncludeTrends()) {
            reportData.put("trends", generateTrendAnalysis(data));
        }

        return reportData;
    }

    private String generateFileName(ReportConfig config, String extension) {
        String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String baseName = config.getTitle().replaceAll("\\s+", "_");
        return String.format("%s_%s.%s", baseName, timestamp, extension);
    }

    private void saveToFile(String fileName, String content) throws IOException {
        File outputDir = new File("reports");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        try (FileWriter writer = new FileWriter(new File(outputDir, fileName))) {
            writer.write(content);
        }
    }

    private String getDataRange(List<LogStatisticsEntry> data) {
        Instant start = data.stream()
                .map(LogStatisticsEntry::getStartTime)
                .min(Instant::compareTo)
                .orElse(Instant.now());

        Instant end = data.stream()
                .map(LogStatisticsEntry::getEndTime)
                .max(Instant::compareTo)
                .orElse(Instant.now());

        return String.format("%s 至 %s",
                start.toString(),
                end.toString());
    }

    private String identifyMainTrend(List<LogStatisticsEntry> data) {
        long growingCount = data.stream()
                .mapToLong(e -> e.getTrendType() == LogStatisticsEntry.TrendType.GROWING ? 1 : 0)
                .sum();

        long decliningCount = data.stream()
                .mapToLong(e -> e.getTrendType() == LogStatisticsEntry.TrendType.DECLINING ? 1 : 0)
                .sum();

        if (growingCount > data.size() * 0.6) {
            return "上升趋势";
        } else if (decliningCount > data.size() * 0.6) {
            return "下降趋势";
        } else {
            return "稳定趋势";
        }
    }

    private Map<String, Object> generateAggregations(List<LogStatisticsEntry> data) {
        Map<String, Object> aggregations = new HashMap<>();

        // 时间维度聚合
        aggregations.put("byTime", aggregator.aggregateByTimeDimension(
                data, StatisticsAggregator.TimeDimension.DAY));

        // 业务维度聚合
        aggregations.put("byBusiness", aggregator.aggregateByBusinessDimension(
                data, "level"));

        return aggregations;
    }

    private Map<String, Object> generateTopNAnalysis(List<LogStatisticsEntry> data) {
        Map<String, Object> topN = new HashMap<>();

        // Top 10 最高数量
        topN.put("highestCount", aggregator.analyzeTopN(
                data, StatisticsAggregator.MetricField.COUNT, 10, false));

        // Top 10 最高增长率
        topN.put("highestGrowth", aggregator.analyzeTopN(
                data, StatisticsAggregator.MetricField.GROWTH_RATE, 10, false));

        return topN;
    }

    private Map<String, Object> generateTrendAnalysis(List<LogStatisticsEntry> data) {
        Map<String, Object> trends = new HashMap<>();

        // 趋势分布
        Map<String, Long> trendDistribution = data.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getTrendType().name(),
                        Collectors.counting()));

        trends.put("distribution", trendDistribution);

        // 异常率分析
        double avgAnomalyRate = data.stream()
                .mapToDouble(LogStatisticsEntry::getAnomalyRate)
                .average()
                .orElse(0.0);

        trends.put("averageAnomalyRate", avgAnomalyRate);

        return trends;
    }

    private String buildHtmlContent(Map<String, Object> reportData, ReportConfig config) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>").append(config.getTitle()).append("</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("table { border-collapse: collapse; width: 100%; margin: 20px 0; }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("th { background-color: #f2f2f2; }\n");
        html.append(".header { text-align: center; color: #333; }\n");
        html.append(".summary { background-color: #f9f9f9; padding: 15px; margin: 20px 0; }\n");
        html.append("</style>\n");
        html.append("</head>\n<body>\n");

        // 报告标题
        html.append("<div class=\"header\">\n");
        html.append("<h1>").append(config.getTitle()).append("</h1>\n");
        html.append("<p>生成时间: ").append(reportData.get("generatedAt")).append("</p>\n");
        html.append("</div>\n");

        // 数据范围
        html.append("<div class=\"summary\">\n");
        html.append("<h3>数据范围</h3>\n");
        html.append("<p>").append(reportData.get("dataRange")).append("</p>\n");
        html.append("</div>\n");

        // 摘要信息
        if (reportData.containsKey("summary")) {
            html.append("<div class=\"summary\">\n");
            html.append("<h3>摘要信息</h3>\n");
            html.append("<table>\n");
            ReportSummary summary = (ReportSummary) reportData.get("summary");
            html.append("<tr><th>指标</th><th>值</th></tr>\n");
            html.append("<tr><td>总条目数</td><td>").append(summary.getTotalEntries()).append("</td></tr>\n");
            html.append("<tr><td>总数量</td><td>").append(String.format("%.2f", summary.getTotalCount()))
                    .append("</td></tr>\n");
            html.append("<tr><td>平均数量</td><td>").append(String.format("%.2f", summary.getAverageCount()))
                    .append("</td></tr>\n");
            html.append("<tr><td>主要趋势</td><td>").append(summary.getMainTrend()).append("</td></tr>\n");
            html.append("</table>\n");
            html.append("</div>\n");
        }

        // 详细数据表
        @SuppressWarnings("unchecked")
        List<LogStatisticsEntry> data = (List<LogStatisticsEntry>) reportData.get("statistics");
        if (data != null && !data.isEmpty()) {
            html.append("<h3>详细统计数据</h3>\n");
            html.append("<table>\n");
            html.append("<tr><th>开始时间</th><th>结束时间</th><th>数量</th><th>平均值</th><th>趋势</th><th>异常率</th></tr>\n");

            data.stream().limit(100).forEach(entry -> {
                html.append("<tr>\n");
                html.append("<td>").append(entry.getStartTime()).append("</td>\n");
                html.append("<td>").append(entry.getEndTime()).append("</td>\n");
                html.append("<td>").append(String.format("%.2f", entry.getCount())).append("</td>\n");
                html.append("<td>").append(String.format("%.2f", entry.getMean())).append("</td>\n");
                html.append("<td>").append(entry.getTrendType()).append("</td>\n");
                html.append("<td>").append(String.format("%.2f%%", entry.getAnomalyRate() * 100)).append("</td>\n");
                html.append("</tr>\n");
            });

            html.append("</table>\n");
        }

        html.append("</body>\n</html>");
        return html.toString();
    }

    // ==================== 数据模型 ====================

    /**
     * 报告配置
     */
    public static class ReportConfig {
        private String title;
        private boolean enableJson = true;
        private boolean enableHtml = true;
        private boolean enablePdf = false;
        private boolean enableExcel = false;
        private boolean includeAggregations = true;
        private boolean includeTopN = true;
        private boolean includeTrends = true;

        // Getters and setters
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public boolean isEnableJson() {
            return enableJson;
        }

        public void setEnableJson(boolean enableJson) {
            this.enableJson = enableJson;
        }

        public boolean isEnableHtml() {
            return enableHtml;
        }

        public void setEnableHtml(boolean enableHtml) {
            this.enableHtml = enableHtml;
        }

        public boolean isEnablePdf() {
            return enablePdf;
        }

        public void setEnablePdf(boolean enablePdf) {
            this.enablePdf = enablePdf;
        }

        public boolean isEnableExcel() {
            return enableExcel;
        }

        public void setEnableExcel(boolean enableExcel) {
            this.enableExcel = enableExcel;
        }

        public boolean isIncludeAggregations() {
            return includeAggregations;
        }

        public void setIncludeAggregations(boolean includeAggregations) {
            this.includeAggregations = includeAggregations;
        }

        public boolean isIncludeTopN() {
            return includeTopN;
        }

        public void setIncludeTopN(boolean includeTopN) {
            this.includeTopN = includeTopN;
        }

        public boolean isIncludeTrends() {
            return includeTrends;
        }

        public void setIncludeTrends(boolean includeTrends) {
            this.includeTrends = includeTrends;
        }
    }

    /**
     * 报告结果
     */
    @lombok.Data
    public static class ReportResult {
        private String format;
        private boolean success;
        private String fileName;
        private String content;
        private String error;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ReportResult result = new ReportResult();

            public Builder format(String format) {
                result.format = format;
                return this;
            }

            public Builder success(boolean success) {
                result.success = success;
                return this;
            }

            public Builder fileName(String fileName) {
                result.fileName = fileName;
                return this;
            }

            public Builder content(String content) {
                result.content = content;
                return this;
            }

            public Builder error(String error) {
                result.error = error;
                return this;
            }

            public ReportResult build() {
                return result;
            }
        }
    }

    /**
     * 多格式报告结果
     */
    @lombok.Data
    public static class MultiFormatReportResult {
        private boolean success;
        private Map<String, ReportResult> reports;
        private int totalFormats;
        private int successfulFormats;
        private String error;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private MultiFormatReportResult result = new MultiFormatReportResult();

            public Builder success(boolean success) {
                result.success = success;
                return this;
            }

            public Builder reports(Map<String, ReportResult> reports) {
                result.reports = reports;
                return this;
            }

            public Builder totalFormats(int totalFormats) {
                result.totalFormats = totalFormats;
                return this;
            }

            public Builder successfulFormats(int successfulFormats) {
                result.successfulFormats = successfulFormats;
                return this;
            }

            public Builder error(String error) {
                result.error = error;
                return this;
            }

            public MultiFormatReportResult build() {
                return result;
            }
        }
    }

    /**
     * 报告摘要
     */
    @lombok.Data
    public static class ReportSummary {
        private int totalEntries;
        private double totalCount;
        private double averageCount;
        private double maxCount;
        private double minCount;
        private Instant startTime;
        private Instant endTime;
        private java.time.Duration duration;
        private String mainTrend;
        private long anomalyCount;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ReportSummary summary = new ReportSummary();

            public Builder totalEntries(int entries) {
                summary.totalEntries = entries;
                return this;
            }

            public Builder totalCount(double count) {
                summary.totalCount = count;
                return this;
            }

            public Builder averageCount(double count) {
                summary.averageCount = count;
                return this;
            }

            public Builder maxCount(double count) {
                summary.maxCount = count;
                return this;
            }

            public Builder minCount(double count) {
                summary.minCount = count;
                return this;
            }

            public Builder startTime(Instant time) {
                summary.startTime = time;
                return this;
            }

            public Builder endTime(Instant time) {
                summary.endTime = time;
                return this;
            }

            public Builder duration(java.time.Duration duration) {
                summary.duration = duration;
                return this;
            }

            public Builder mainTrend(String trend) {
                summary.mainTrend = trend;
                return this;
            }

            public Builder anomalyCount(long count) {
                summary.anomalyCount = count;
                return this;
            }

            public ReportSummary build() {
                return summary;
            }
        }
    }
}
