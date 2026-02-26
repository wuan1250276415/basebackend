package com.basebackend.database.dialect;

import com.baomidou.mybatisplus.annotation.DbType;
import com.basebackend.database.statistics.model.SqlOptimizationSuggestion;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface DatabaseDialect {

    void switchSchema(Connection connection, String schemaName) throws SQLException;

    List<SqlOptimizationSuggestion> parseExplainResult(String sqlMd5, List<Map<String, Object>> explainRows);

    String formatExplainResult(List<Map<String, Object>> explainRows);

    DbType getMyBatisPlusDbType();

    String getDruidDbType();
}
