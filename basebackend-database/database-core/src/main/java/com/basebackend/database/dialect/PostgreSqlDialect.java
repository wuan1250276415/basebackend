package com.basebackend.database.dialect;

import com.baomidou.mybatisplus.annotation.DbType;
import com.basebackend.database.statistics.model.SqlOptimizationSuggestion;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostgreSqlDialect implements DatabaseDialect {

    private static final Pattern ROWS_PATTERN = Pattern.compile("rows=(\\d+)");

    @Override
    public void switchSchema(Connection connection, String schemaName) throws SQLException {
        connection.createStatement().execute("SET search_path TO " + schemaName);
    }

    @Override
    public List<SqlOptimizationSuggestion> parseExplainResult(String sqlMd5, List<Map<String, Object>> explainRows) {
        List<SqlOptimizationSuggestion> suggestions = new ArrayList<>();
        for (Map<String, Object> row : explainRows) {
            String queryPlan = (String) row.get("QUERY PLAN");
            if (queryPlan == null) continue;
            String planLower = queryPlan.toLowerCase();
            if (planLower.contains("seq scan")) {
                suggestions.add(SqlOptimizationSuggestion.builder()
                        .sqlMd5(sqlMd5)
                        .severity(SqlOptimizationSuggestion.Severity.HIGH)
                        .category(SqlOptimizationSuggestion.Category.INDEX)
                        .issue("Sequential scan detected")
                        .suggestion("Consider adding an index on the columns used in WHERE clause")
                        .executionPlan(formatExplainResult(explainRows))
                        .build());
            }
            if (planLower.contains("sort")) {
                suggestions.add(SqlOptimizationSuggestion.builder()
                        .sqlMd5(sqlMd5)
                        .severity(SqlOptimizationSuggestion.Severity.MEDIUM)
                        .category(SqlOptimizationSuggestion.Category.INDEX)
                        .issue("Sort operation detected")
                        .suggestion("Consider adding an index on the ORDER BY columns")
                        .executionPlan(formatExplainResult(explainRows))
                        .build());
            }
            if (planLower.contains("hash") && planLower.contains("aggregate")) {
                suggestions.add(SqlOptimizationSuggestion.builder()
                        .sqlMd5(sqlMd5)
                        .severity(SqlOptimizationSuggestion.Severity.MEDIUM)
                        .category(SqlOptimizationSuggestion.Category.QUERY_STRUCTURE)
                        .issue("Hash aggregate detected")
                        .suggestion("Consider optimizing GROUP BY or DISTINCT operations")
                        .executionPlan(formatExplainResult(explainRows))
                        .build());
            }
            Matcher matcher = ROWS_PATTERN.matcher(queryPlan);
            if (matcher.find()) {
                long estimatedRows = Long.parseLong(matcher.group(1));
                if (estimatedRows > 10000) {
                    suggestions.add(SqlOptimizationSuggestion.builder()
                            .sqlMd5(sqlMd5)
                            .severity(SqlOptimizationSuggestion.Severity.MEDIUM)
                            .category(SqlOptimizationSuggestion.Category.INDEX)
                            .issue("Large number of estimated rows: " + estimatedRows)
                            .suggestion("Consider adding more selective indexes or refining WHERE conditions")
                            .executionPlan(formatExplainResult(explainRows))
                            .build());
                }
            }
        }
        return suggestions;
    }

    @Override
    public String formatExplainResult(List<Map<String, Object>> explainRows) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> row : explainRows) {
            Object plan = row.get("QUERY PLAN");
            if (plan != null) {
                sb.append(plan).append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public DbType getMyBatisPlusDbType() {
        return DbType.POSTGRE_SQL;
    }

    @Override
    public String getDruidDbType() {
        return "postgresql";
    }
}
