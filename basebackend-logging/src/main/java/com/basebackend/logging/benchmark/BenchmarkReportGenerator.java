package com.basebackend.logging.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 性能基准测试报告生成器
 *
 * 生成详细的性能测试报告：
 * - HTML 格式报告（可视化图表）
 * - JSON 格式报告（机器可读）
 * - Markdown 格式报告（文档友好）
 * - CSV 格式报告（数据分析）
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
@Component
public class BenchmarkReportGenerator {

    private final ObjectMapper objectMapper;

    public BenchmarkReportGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 生成完整测试报告
     *
     * @param results 测试结果列表
     * @param outputDir 输出目录
     * @return 报告文件路径
     */
    public List<String> generateFullReport(List<BenchmarkTestResult> results, String outputDir) {
        log.info("生成完整性能测试报告: {} 个测试结果", results.size());

        List<String> reportFiles = new ArrayList<>();

        try {
            // 生成 JSON 报告
            String jsonReport = generateJsonReport(results);
            String jsonFile = saveToFile(outputDir, "performance-report.json", jsonReport);
            reportFiles.add(jsonFile);

            // 生成 Markdown 报告
            String mdReport = generateMarkdownReport(results);
            String mdFile = saveToFile(outputDir, "performance-report.md", mdReport);
            reportFiles.add(mdFile);

            // 生成 HTML 报告
            String htmlReport = generateHtmlReport(results);
            String htmlFile = saveToFile(outputDir, "performance-report.html", htmlReport);
            reportFiles.add(htmlFile);

            // 生成 CSV 报告
            String csvReport = generateCsvReport(results);
            String csvFile = saveToFile(outputDir, "performance-report.csv", csvReport);
            reportFiles.add(csvFile);

            log.info("完整性能测试报告生成完成: {} 个文件", reportFiles.size());

        } catch (Exception e) {
            log.error("生成性能测试报告失败", e);
        }

        return reportFiles;
    }

    /**
     * 生成 JSON 格式报告
     *
     * @param results 测试结果
     * @return JSON 字符串
     */
    public String generateJsonReport(List<BenchmarkTestResult> results) {
        Map<String, Object> reportData = new HashMap<>();

        reportData.put("reportTitle", "性能基准测试报告");
        reportData.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        reportData.put("testCount", results.size());
        reportData.put("summary", generateSummary(results));
        reportData.put("results", results);
        reportData.put("recommendations", generateRecommendations(results));

        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(reportData);
        } catch (Exception e) {
            log.error("JSON 报告生成失败", e);
            return "{}";
        }
    }

    /**
     * 生成 Markdown 格式报告
     *
     * @param results 测试结果
     * @return Markdown 字符串
     */
    public String generateMarkdownReport(List<BenchmarkTestResult> results) {
        StringBuilder md = new StringBuilder();

        // 标题
        md.append("# 性能基准测试报告\n\n");
        md.append("生成时间: ").append(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");

        // 摘要
        md.append("## 执行摘要\n\n");
        Map<String, Object> summary = generateSummary(results);
        md.append("- 总测试数: ").append(summary.get("totalTests")).append("\n");
        md.append("- 平均 TPS: ").append(String.format("%.2f", (Double) summary.get("avgTps"))).append("\n");
        md.append("- 平均延迟 (P95): ").append(String.format("%.2f", (Double) summary.get("avgP95Latency"))).append(" ms\n");
        md.append("- 平均成功率: ").append(String.format("%.2f%%", (Double) summary.get("avgSuccessRate"))).append("\n\n");

        // 详细结果
        md.append("## 详细测试结果\n\n");
        md.append("| 测试名称 | 类型 | TPS | P95延迟 | 成功率 | 状态 |\n");
        md.append("|----------|------|-----|---------|--------|------|\n");

        for (BenchmarkTestResult result : results) {
            String status = getStatusIcon(result);
            md.append("| ").append(result.getTestName())
              .append(" | ").append(result.getTestType())
              .append(" | ").append(String.format("%.2f", result.getTps()))
              .append(" | ").append(String.format("%.2f", result.getP95LatencyMs()))
              .append(" | ").append(String.format("%.2f%%", result.getSuccessRate()))
              .append(" | ").append(status).append(" |\n");
        }

        // 性能对比
        md.append("\n## 性能对比分析\n\n");
        md.append(generatePerformanceComparison(results));

        // 优化建议
        md.append("\n## 优化建议\n\n");
        List<String> recommendations = generateRecommendations(results);
        for (int i = 0; i < recommendations.size(); i++) {
            md.append(i + 1).append(". ").append(recommendations.get(i)).append("\n");
        }

        // 附录
        md.append("\n## 附录\n\n");
        md.append("### 测试环境\n");
        md.append("- Java 版本: ").append(System.getProperty("java.version")).append("\n");
        md.append("- 操作系统: ").append(System.getProperty("os.name")).append("\n");
        md.append("- CPU 核心数: ").append(Runtime.getRuntime().availableProcessors()).append("\n");
        md.append("- 最大内存: ").append(Runtime.getRuntime().maxMemory() / (1024 * 1024)).append(" MB\n");

        return md.toString();
    }

    /**
     * 生成 HTML 格式报告
     *
     * @param results 测试结果
     * @return HTML 字符串
     */
    public String generateHtmlReport(List<BenchmarkTestResult> results) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>性能基准测试报告</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 40px; }\n");
        html.append(".container { max-width: 1200px; margin: 0 auto; }\n");
        html.append("h1 { color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }\n");
        html.append("h2 { color: #34495e; margin-top: 30px; }\n");
        html.append("table { border-collapse: collapse; width: 100%; margin: 20px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
        html.append("th { background-color: #3498db; color: white; padding: 12px; text-align: left; }\n");
        html.append("td { padding: 10px; border-bottom: 1px solid #ddd; }\n");
        html.append("tr:hover { background-color: #f5f5f5; }\n");
        html.append(".summary { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px; margin: 20px 0; }\n");
        html.append(".metric { display: inline-block; margin: 10px 20px; }\n");
        html.append(".metric-value { font-size: 2em; font-weight: bold; }\n");
        html.append(".metric-label { font-size: 0.9em; opacity: 0.9; }\n");
        html.append(".success { color: #27ae60; }\n");
        html.append(".warning { color: #f39c12; }\n");
        html.append(".error { color: #e74c3c; }\n");
        html.append(".chart-container { margin: 20px 0; padding: 20px; background: #f8f9fa; border-radius: 8px; }\n");
        html.append("</style>\n");
        html.append("</head>\n<body>\n");

        html.append("<div class=\"container\">\n");

        // 标题
        html.append("<h1>🚀 性能基准测试报告</h1>\n");
        html.append("<p><strong>生成时间:</strong> ").append(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>\n");

        // 摘要卡片
        Map<String, Object> summary = generateSummary(results);
        html.append("<div class=\"summary\">\n");
        html.append("<h2>📊 执行摘要</h2>\n");
        html.append("<div class=\"metric\">\n");
        html.append("<div class=\"metric-value\">").append(summary.get("totalTests")).append("</div>\n");
        html.append("<div class=\"metric-label\">总测试数</div>\n");
        html.append("</div>\n");
        html.append("<div class=\"metric\">\n");
        html.append("<div class=\"metric-value\">").append(String.format("%.2f", (Double) summary.get("avgTps"))).append("</div>\n");
        html.append("<div class=\"metric-label\">平均 TPS</div>\n");
        html.append("</div>\n");
        html.append("<div class=\"metric\">\n");
        html.append("<div class=\"metric-value\">").append(String.format("%.2f", (Double) summary.get("avgP95Latency"))).append(" ms</div>\n");
        html.append("<div class=\"metric-label\">P95 平均延迟</div>\n");
        html.append("</div>\n");
        html.append("<div class=\"metric\">\n");
        html.append("<div class=\"metric-value\">").append(String.format("%.2f%%", (Double) summary.get("avgSuccessRate"))).append("</div>\n");
        html.append("<div class=\"metric-label\">平均成功率</div>\n");
        html.append("</div>\n");
        html.append("</div>\n");

        // 详细结果表
        html.append("<h2>📋 详细测试结果</h2>\n");
        html.append("<table>\n");
        html.append("<thead>\n");
        html.append("<tr>\n");
        html.append("<th>测试名称</th><th>类型</th><th>TPS</th><th>P95延迟</th><th>成功率</th><th>状态</th>\n");
        html.append("</tr>\n");
        html.append("</thead>\n");
        html.append("<tbody>\n");

        for (BenchmarkTestResult result : results) {
            String statusClass = getStatusClass(result);
            String status = getStatusIcon(result);

            html.append("<tr>\n");
            html.append("<td>").append(result.getTestName()).append("</td>\n");
            html.append("<td>").append(result.getTestType()).append("</td>\n");
            html.append("<td>").append(String.format("%.2f", result.getTps())).append("</td>\n");
            html.append("<td>").append(String.format("%.2f", result.getP95LatencyMs())).append(" ms</td>\n");
            html.append("<td class=\"").append(statusClass).append("\">")
              .append(String.format("%.2f%%", result.getSuccessRate())).append("</td>\n");
            html.append("<td>").append(status).append("</td>\n");
            html.append("</tr>\n");
        }

        html.append("</tbody>\n");
        html.append("</table>\n");

        // 优化建议
        html.append("<h2>💡 优化建议</h2>\n");
        html.append("<ul>\n");
        for (String recommendation : generateRecommendations(results)) {
            html.append("<li>").append(recommendation).append("</li>\n");
        }
        html.append("</ul>\n");

        html.append("</div>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    /**
     * 生成 CSV 格式报告
     *
     * @param results 测试结果
     * @return CSV 字符串
     */
    public String generateCsvReport(List<BenchmarkTestResult> results) {
        StringBuilder csv = new StringBuilder();

        // 表头
        csv.append("测试名称,类型,TPS,P95延迟(ms),成功率(%),状态\n");

        // 数据行
        for (BenchmarkTestResult result : results) {
            csv.append("\"").append(result.getTestName()).append("\",")
              .append(result.getTestType()).append(",")
              .append(String.format("%.2f", result.getTps())).append(",")
              .append(String.format("%.2f", result.getP95LatencyMs())).append(",")
              .append(String.format("%.2f", result.getSuccessRate())).append(",")
              .append(getStatusText(result)).append("\n");
        }

        return csv.toString();
    }

    // ==================== 私有辅助方法 ====================

    private Map<String, Object> generateSummary(List<BenchmarkTestResult> results) {
        Map<String, Object> summary = new HashMap<>();

        summary.put("totalTests", results.size());
        summary.put("avgTps", results.stream()
                .mapToDouble(BenchmarkTestResult::getTps)
                .average()
                .orElse(0.0));
        summary.put("avgP95Latency", results.stream()
                .mapToDouble(BenchmarkTestResult::getP95LatencyMs)
                .average()
                .orElse(0.0));
        summary.put("avgSuccessRate", results.stream()
                .mapToDouble(BenchmarkTestResult::getSuccessRate)
                .average()
                .orElse(0.0));

        return summary;
    }

    private List<String> generateRecommendations(List<BenchmarkTestResult> results) {
        List<String> recommendations = new ArrayList<>();

        // 分析 TPS
        double avgTps = results.stream()
                .mapToDouble(BenchmarkTestResult::getTps)
                .average()
                .orElse(0.0);

        if (avgTps < 1000) {
            recommendations.add("系统吞吐量较低，建议优化算法或增加并发处理能力");
        } else {
            recommendations.add("系统吞吐量表现良好，保持当前配置");
        }

        // 分析延迟
        double avgP95 = results.stream()
                .mapToDouble(BenchmarkTestResult::getP95LatencyMs)
                .average()
                .orElse(0.0);

        if (avgP95 > 100) {
            recommendations.add("P95 延迟过高，建议优化慢查询或增加缓存");
        }

        // 分析成功率
        double avgSuccessRate = results.stream()
                .mapToDouble(BenchmarkTestResult::getSuccessRate)
                .average()
                .orElse(0.0);

        if (avgSuccessRate < 99.0) {
            recommendations.add("成功率低于 99%，建议检查错误日志并优化异常处理");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("所有性能指标表现良好，无需特别优化");
        }

        return recommendations;
    }

    private String generatePerformanceComparison(List<BenchmarkTestResult> results) {
        StringBuilder comparison = new StringBuilder();

        // 按 TPS 排序
        List<BenchmarkTestResult> sortedByTps = new ArrayList<>(results);
        sortedByTps.sort(Comparator.comparingDouble(BenchmarkTestResult::getTps).reversed());

        comparison.append("### 吞吐量排名\n\n");
        for (int i = 0; i < Math.min(5, sortedByTps.size()); i++) {
            BenchmarkTestResult result = sortedByTps.get(i);
            comparison.append(i + 1).append(". ")
                    .append(result.getTestName())
                    .append(": ")
                    .append(String.format("%.2f TPS", result.getTps()))
                    .append("\n");
        }

        return comparison.toString();
    }

    private String getStatusIcon(BenchmarkTestResult result) {
        if (result.getSuccessRate() >= 99.0 && result.getP95LatencyMs() < 100) {
            return "✅ 优秀";
        } else if (result.getSuccessRate() >= 95.0 && result.getP95LatencyMs() < 200) {
            return "⚠️ 良好";
        } else {
            return "❌ 需优化";
        }
    }

    private String getStatusClass(BenchmarkTestResult result) {
        if (result.getSuccessRate() >= 99.0 && result.getP95LatencyMs() < 100) {
            return "success";
        } else if (result.getSuccessRate() >= 95.0 && result.getP95LatencyMs() < 200) {
            return "warning";
        } else {
            return "error";
        }
    }

    private String getStatusText(BenchmarkTestResult result) {
        if (result.getSuccessRate() >= 99.0 && result.getP95LatencyMs() < 100) {
            return "优秀";
        } else if (result.getSuccessRate() >= 95.0 && result.getP95LatencyMs() < 200) {
            return "良好";
        } else {
            return "需优化";
        }
    }

    private String saveToFile(String outputDir, String fileName, String content) throws Exception {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dir, fileName);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }

        log.debug("报告文件已保存: {}", file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    // ==================== 数据模型 ====================

    /**
     * 基准测试结果
     */
    public static class BenchmarkTestResult {
        private String testName;
        private String testType;
        private double tps;
        private double avgLatencyMs;
        private double p95LatencyMs;
        private double successRate;
        private long durationMs;

        public String getTestName() { return testName; }
        public void setTestName(String testName) { this.testName = testName; }

        public String getTestType() { return testType; }
        public void setTestType(String testType) { this.testType = testType; }

        public double getTps() { return tps; }
        public void setTps(double tps) { this.tps = tps; }

        public double getAvgLatencyMs() { return avgLatencyMs; }
        public void setAvgLatencyMs(double avgLatencyMs) { this.avgLatencyMs = avgLatencyMs; }

        public double getP95LatencyMs() { return p95LatencyMs; }
        public void setP95LatencyMs(double p95LatencyMs) { this.p95LatencyMs = p95LatencyMs; }

        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }

        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    }
}
