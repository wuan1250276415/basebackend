package com.basebackend.database.dialect;

import com.baomidou.mybatisplus.annotation.DbType;
import com.basebackend.database.statistics.model.SqlOptimizationSuggestion;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MySqlDialect implements DatabaseDialect {

    @Override
    public void switchSchema(Connection connection, String schemaName) throws SQLException {
        connection.createStatement().execute("USE `" + schemaName + "`");
    }

    @Override
    public List<SqlOptimizationSuggestion> parseExplainResult(String sqlMd5, List<Map<String, Object>> explainRows) {
        List<SqlOptimizationSuggestion> suggestions = new ArrayList<>();
        for (Map<String, Object> row : explainRows) {
            String type = (String) row.get("type");
            if ("ALL".equals(type)) {
                suggestions.add(SqlOptimizationSuggestion.builder()
                        .sqlMd5(sqlMd5)
                        .severity(SqlOptimizationSuggestion.Severity.HIGH)
                        .category(SqlOptimizationSuggestion.Category.INDEX)
                        .issue("Full table scan detected")
                        .suggestion("Consider adding an index on the columns used in WHERE clause")
                        .executionPlan(formatExplainResult(explainRows))
                        .build());
            }
            String extra = (String) row.get("Extra");
            if (extra != null && extra.contains("Using filesort")) {
                suggestions.add(SqlOptimizationSuggestion.builder()
                        .sqlMd5(sqlMd5)
                        .severity(SqlOptimizationSuggestion.Severity.MEDIUM)
                        .category(SqlOptimizationSuggestion.Category.INDEX)
                        .issue("Filesort operation detected")
                        .suggestion("Consider adding an index on the ORDER BY columns")
                        .executionPlan(formatExplainResult(explainRows))
                        .build());
            }
            if (extra != null && extra.contains("Using temporary")) {
                suggestions.add(SqlOptimizationSuggestion.builder()
                        .sqlMd5(sqlMd5)
                        .severity(SqlOptimizationSuggestion.Severity.MEDIUM)
                        .category(SqlOptimizationSuggestion.Category.QUERY_STRUCTURE)
                        .issue("Temporary table creation detected")
                        .suggestion("Consider optimizing GROUP BY or DISTINCT operations")
                        .executionPlan(formatExplainResult(explainRows))
                        .build());
            }
            Object rows = row.get("rows");
            if (rows != null && ((Number) rows).longValue() > 10000) {
                suggestions.add(SqlOptimizationSuggestion.builder()
                        .sqlMd5(sqlMd5)
                        .severity(SqlOptimizationSuggestion.Severity.MEDIUM)
                        .category(SqlOptimizationSuggestion.Category.INDEX)
                        .issue("Large number of rows examined: " + rows)
                        .suggestion("Consider adding more selective indexes or refining WHERE conditions")
                        .executionPlan(formatExplainResult(explainRows))
                        .build());
            }
        }
        return suggestions;
    }

    @Override
    public String formatExplainResult(List<Map<String, Object>> explainRows) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> row : explainRows) {
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

    @Override
    public DbType getMyBatisPlusDbType() {
        return DbType.MYSQL;
    }

    @Override
    public String getDruidDbType() {
        return "mysql";
    }
}
