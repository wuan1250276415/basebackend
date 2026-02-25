package com.basebackend.database.statistics.analyzer;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.statistics.entity.SqlStatistics;
import com.basebackend.database.statistics.model.SqlOptimizationSuggestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * SQL性能分析器
 * 分析SQL执行计划并提供优化建议
 */
@Slf4j
@Component
public class SqlPerformanceAnalyzer {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseEnhancedProperties properties;

    // SQL pattern matchers for common issues
    private static final Pattern SELECT_ALL_PATTERN = Pattern.compile("SELECT\\s+\\*", Pattern.CASE_INSENSITIVE);
    private static final Pattern NO_WHERE_PATTERN = Pattern.compile("SELECT.*FROM.*(?!WHERE)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern LIKE_PREFIX_PATTERN = Pattern.compile("LIKE\\s+'%[^']*'", Pattern.CASE_INSENSITIVE);
    private static final Pattern FUNCTION_IN_WHERE_PATTERN = Pattern.compile("WHERE.*\\w+\\([^)]*\\)\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern OR_PATTERN = Pattern.compile("WHERE.*\\bOR\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SUBQUERY_PATTERN = Pattern.compile("\\(\\s*SELECT", Pattern.CASE_INSENSITIVE);

    public SqlPerformanceAnalyzer(JdbcTemplate jdbcTemplate, DatabaseEnhancedProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    /**
     * 分析SQL性能并提供优化建议
     *
     * @param statistics SQL统计信息
     * @return 优化建议列表
     */
    public List<SqlOptimizationSuggestion> analyze(SqlStatistics statistics) {
        List<SqlOptimizationSuggestion> suggestions = new ArrayList<>();

        // 1. Analyze execution plan if enabled
        if (properties.getSqlStatistics().isExplainEnabled()) {
            suggestions.addAll(analyzeExecutionPlan(statistics));
        }

        // 2. Analyze SQL patterns
        suggestions.addAll(analyzeSqlPatterns(statistics));

        // 3. Analyze performance metrics
        suggestions.addAll(analyzePerformanceMetrics(statistics));

        return suggestions;
    }

    /**
     * 分析SQL执行计划
     */
    private List<SqlOptimizationSuggestion> analyzeExecutionPlan(SqlStatistics statistics) {
        List<SqlOptimizationSuggestion> suggestions = new ArrayList<>();

        try {
            String explainSql = "EXPLAIN " + statistics.getSqlTemplate();
            List<Map<String, Object>> explainResult = jdbcTemplate.queryForList(explainSql);

            for (Map<String, Object> row : explainResult) {
                // Check for full table scan
                String type = (String) row.get("type");
                if ("ALL".equals(type)) {
                    suggestions.add(SqlOptimizationSuggestion.builder()
                            .sqlMd5(statistics.getSqlMd5())
                            .severity(SqlOptimizationSuggestion.Severity.HIGH)
                            .category(SqlOptimizationSuggestion.Category.INDEX)
                            .issue("Full table scan detected")
                            .suggestion("Consider adding an index on the columns used in WHERE clause")
                            .executionPlan(formatExecutionPlan(explainResult))
                            .build());
                }

                // Check for filesort
                String extra = (String) row.get("Extra");
                if (extra != null && extra.contains("Using filesort")) {
                    suggestions.add(SqlOptimizationSuggestion.builder()
                            .sqlMd5(statistics.getSqlMd5())
                            .severity(SqlOptimizationSuggestion.Severity.MEDIUM)
                            .category(SqlOptimizationSuggestion.Category.INDEX)
                            .issue("Filesort operation detected")
                            .suggestion("Consider adding an index on the ORDER BY columns")
                            .executionPlan(formatExecutionPlan(explainResult))
                            .build());
                }

                // Check for temporary table
                if (extra != null && extra.contains("Using temporary")) {
                    suggestions.add(SqlOptimizationSuggestion.builder()
                            .sqlMd5(statistics.getSqlMd5())
                            .severity(SqlOptimizationSuggestion.Severity.MEDIUM)
                            .category(SqlOptimizationSuggestion.Category.QUERY_STRUCTURE)
                            .issue("Temporary table creation detected")
                            .suggestion("Consider optimizing GROUP BY or DISTINCT operations")
                            .executionPlan(formatExecutionPlan(explainResult))
                            .build());
                }

                // Check rows examined
                Object rows = row.get("rows");
                if (rows != null && ((Number) rows).longValue() > 10000) {
                    suggestions.add(SqlOptimizationSuggestion.builder()
                            .sqlMd5(statistics.getSqlMd5())
                            .severity(SqlOptimizationSuggestion.Severity.MEDIUM)
                            .category(SqlOptimizationSuggestion.Category.INDEX)
                            .issue("Large number of rows examined: " + rows)
                            .suggestion("Consider adding more selective indexes or refining WHERE conditions")
                            .executionPlan(formatExecutionPlan(explainResult))
                            .build());
                }
            }

        } catch (Exception e) {
            log.warn("Failed to analyze execution plan for SQL: {}", statistics.getSqlMd5(), e);
        }

        return suggestions;
    }

    /**
     * 分析SQL语句模式
     */
    private List<SqlOptimizationSuggestion> analyzeSqlPatterns(SqlStatistics statistics) {
        List<SqlOptimizationSuggestion> suggestions = new ArrayList<>();
        String sql = statistics.getSqlTemplate();

        // Check for SELECT *
        if (SELECT_ALL_PATTERN.matcher(sql).find()) {
            suggestions.add(SqlOptimizationSuggestion.builder()
                    .sqlMd5(statistics.getSqlMd5())
                    .severity(SqlOptimizationSuggestion.Severity.LOW)
                    .category(SqlOptimizationSuggestion.Category.QUERY_STRUCTURE)
                    .issue("SELECT * detected")
                    .suggestion("Specify only the columns you need instead of using SELECT *")
                    .build());
        }

        // Check for queries without WHERE clause
        if (sql.toUpperCase().contains("SELECT") && 
            sql.toUpperCase().contains("FROM") && 
            !sql.toUpperCase().contains("WHERE") &&
            !sql.toUpperCase().contains("LIMIT")) {
            suggestions.add(SqlOptimizationSuggestion.builder()
                    .sqlMd5(statistics.getSqlMd5())
                    .severity(SqlOptimizationSuggestion.Severity.HIGH)
                    .category(SqlOptimizationSuggestion.Category.QUERY_STRUCTURE)
                    .issue("Query without WHERE clause")
                    .suggestion("Add WHERE clause to filter data or use LIMIT to restrict result set")
                    .build());
        }

        // Check for leading wildcard in LIKE
        if (LIKE_PREFIX_PATTERN.matcher(sql).find()) {
            suggestions.add(SqlOptimizationSuggestion.builder()
                    .sqlMd5(statistics.getSqlMd5())
                    .severity(SqlOptimizationSuggestion.Severity.MEDIUM)
                    .category(SqlOptimizationSuggestion.Category.INDEX)
                    .issue("Leading wildcard in LIKE clause")
                    .suggestion("Avoid leading wildcards (LIKE '%...') as they prevent index usage")
                    .build());
        }

        // Check for functions in WHERE clause
        if (FUNCTION_IN_WHERE_PATTERN.matcher(sql).find()) {
            suggestions.add(SqlOptimizationSuggestion.builder()
                    .sqlMd5(statistics.getSqlMd5())
                    .severity(SqlOptimizationSuggestion.Severity.MEDIUM)
                    .category(SqlOptimizationSuggestion.Category.INDEX)
                    .issue("Function applied to column in WHERE clause")
                    .suggestion("Avoid applying functions to indexed columns in WHERE clause")
                    .build());
        }

        // Check for OR conditions
        if (OR_PATTERN.matcher(sql).find()) {
            suggestions.add(SqlOptimizationSuggestion.builder()
                    .sqlMd5(statistics.getSqlMd5())
                    .severity(SqlOptimizationSuggestion.Severity.LOW)
                    .category(SqlOptimizationSuggestion.Category.QUERY_STRUCTURE)
                    .issue("OR condition in WHERE clause")
                    .suggestion("Consider using UNION or IN clause instead of OR for better performance")
                    .build());
        }

        // Check for subqueries
        if (SUBQUERY_PATTERN.matcher(sql).find()) {
            suggestions.add(SqlOptimizationSuggestion.builder()
                    .sqlMd5(statistics.getSqlMd5())
                    .severity(SqlOptimizationSuggestion.Severity.LOW)
                    .category(SqlOptimizationSuggestion.Category.QUERY_STRUCTURE)
                    .issue("Subquery detected")
                    .suggestion("Consider using JOIN instead of subquery for better performance")
                    .build());
        }

        return suggestions;
    }

    /**
     * 分析性能指标
     */
    private List<SqlOptimizationSuggestion> analyzePerformanceMetrics(SqlStatistics statistics) {
        List<SqlOptimizationSuggestion> suggestions = new ArrayList<>();

        // Check average execution time
        if (statistics.getAvgTime() > 1000) {
            suggestions.add(SqlOptimizationSuggestion.builder()
                    .sqlMd5(statistics.getSqlMd5())
                    .severity(SqlOptimizationSuggestion.Severity.HIGH)
                    .category(SqlOptimizationSuggestion.Category.PERFORMANCE)
                    .issue("High average execution time: " + statistics.getAvgTime() + "ms")
                    .suggestion("This query is slow. Review execution plan and consider optimization")
                    .build());
        }

        // Check execution frequency
        if (statistics.getExecuteCount() > 10000) {
            suggestions.add(SqlOptimizationSuggestion.builder()
                    .sqlMd5(statistics.getSqlMd5())
                    .severity(SqlOptimizationSuggestion.Severity.MEDIUM)
                    .category(SqlOptimizationSuggestion.Category.CACHING)
                    .issue("High execution frequency: " + statistics.getExecuteCount() + " times")
                    .suggestion("Consider caching the results of this frequently executed query")
                    .build());
        }

        // Check failure rate
        if (statistics.getFailCount() > 0) {
            double failureRate = (double) statistics.getFailCount() / statistics.getExecuteCount();
            if (failureRate > 0.01) { // More than 1% failure rate
                suggestions.add(SqlOptimizationSuggestion.builder()
                        .sqlMd5(statistics.getSqlMd5())
                        .severity(SqlOptimizationSuggestion.Severity.HIGH)
                        .category(SqlOptimizationSuggestion.Category.ERROR)
                        .issue("High failure rate: " + String.format("%.2f%%", failureRate * 100))
                        .suggestion("Investigate the cause of failures and add proper error handling")
                        .build());
            }
        }

        // Check time variance
        if (statistics.getMaxTime() > statistics.getAvgTime() * 10) {
            suggestions.add(SqlOptimizationSuggestion.builder()
                    .sqlMd5(statistics.getSqlMd5())
                    .severity(SqlOptimizationSuggestion.Severity.MEDIUM)
                    .category(SqlOptimizationSuggestion.Category.PERFORMANCE)
                    .issue("High execution time variance (max: " + statistics.getMaxTime() + "ms, avg: " + statistics.getAvgTime() + "ms)")
                    .suggestion("Inconsistent performance detected. Check for data skew or resource contention")
                    .build());
        }

        return suggestions;
    }

    /**
     * 格式化执行计划
     */
    private String formatExecutionPlan(List<Map<String, Object>> explainResult) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> row : explainResult) {
            sb.append("Table: ").append(row.get("table"))
              .append(", Type: ").append(row.get("type"))
              .append(", Possible Keys: ").append(row.get("possible_keys"))
              .append(", Key: ").append(row.get("key"))
              .append(", Rows: ").append(row.get("rows"))
              .append(", Extra: ").append(row.get("Extra"))
              .append("\n");
        }
        return sb.toString();
    }

    /**
     * 批量分析多个SQL统计
     */
    public List<SqlOptimizationSuggestion> analyzeBatch(List<SqlStatistics> statisticsList) {
        List<SqlOptimizationSuggestion> allSuggestions = new ArrayList<>();
        
        for (SqlStatistics statistics : statisticsList) {
            try {
                List<SqlOptimizationSuggestion> suggestions = analyze(statistics);
                allSuggestions.addAll(suggestions);
            } catch (Exception e) {
                log.error("Failed to analyze SQL statistics: {}", statistics.getSqlMd5(), e);
            }
        }
        
        return allSuggestions;
    }
}
