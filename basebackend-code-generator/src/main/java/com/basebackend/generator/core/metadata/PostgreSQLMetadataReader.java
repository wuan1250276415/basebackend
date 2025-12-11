package com.basebackend.generator.core.metadata;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL元数据读取器
 * 
 * 支持PostgreSQL数据库的表结构读取和元数据提取
 */
@Slf4j
@Component
public class PostgreSQLMetadataReader extends AbstractDatabaseMetadataReader {

    @Override
    public List<String> getTableNames(DataSource dataSource, String schema) {
        List<String> tables = new ArrayList<>();
        String actualSchema = StrUtil.isBlank(schema) ? "public" : schema;

        String sql = "SELECT table_name FROM information_schema.tables " +
                "WHERE table_schema = ? AND table_type = 'BASE TABLE' " +
                "ORDER BY table_name";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, actualSchema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("table_name"));
                }
            }
        } catch (SQLException e) {
            log.error("PostgreSQL获取表列表失败", e);
            throw new RuntimeException("获取表列表失败: " + e.getMessage(), e);
        }

        return tables;
    }

    @Override
    public List<ColumnMetadata> getColumns(DataSource dataSource, String tableName) {
        List<ColumnMetadata> columns = new ArrayList<>();

        // PostgreSQL列信息查询，包含注释
        String sql = """
                SELECT
                    c.column_name,
                    c.data_type,
                    c.udt_name,
                    c.is_nullable,
                    c.column_default,
                    c.character_maximum_length,
                    c.numeric_precision,
                    c.numeric_scale,
                    COALESCE(col_description((c.table_schema || '.' || c.table_name)::regclass::oid, c.ordinal_position), c.column_name) as column_comment,
                    CASE WHEN pk.column_name IS NOT NULL THEN true ELSE false END as is_primary_key,
                    CASE WHEN c.column_default LIKE 'nextval%' THEN true ELSE false END as is_auto_increment
                FROM information_schema.columns c
                LEFT JOIN (
                    SELECT kcu.column_name
                    FROM information_schema.table_constraints tc
                    JOIN information_schema.key_column_usage kcu
                        ON tc.constraint_name = kcu.constraint_name
                        AND tc.table_schema = kcu.table_schema
                    WHERE tc.constraint_type = 'PRIMARY KEY'
                        AND tc.table_name = ?
                ) pk ON c.column_name = pk.column_name
                WHERE c.table_name = ?
                ORDER BY c.ordinal_position
                """;

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tableName);
            ps.setString(2, tableName);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String columnName = rs.getString("column_name");
                    String dataType = rs.getString("udt_name").toLowerCase();
                    String columnComment = rs.getString("column_comment");
                    boolean nullable = "YES".equalsIgnoreCase(rs.getString("is_nullable"));
                    boolean isPrimaryKey = rs.getBoolean("is_primary_key");
                    boolean isAutoIncrement = rs.getBoolean("is_auto_increment");
                    String defaultValue = rs.getString("column_default");
                    Integer maxLength = rs.getObject("character_maximum_length") != null
                            ? rs.getInt("character_maximum_length")
                            : null;

                    ColumnMetadata column = ColumnMetadata.builder()
                            .columnName(columnName)
                            .columnType(normalizeDataType(dataType))
                            .columnComment(StrUtil.isNotBlank(columnComment) ? columnComment : columnName)
                            .isPrimaryKey(isPrimaryKey)
                            .isAutoIncrement(isAutoIncrement)
                            .nullable(nullable)
                            .isSystemField(isSystemField(columnName))
                            .queryable(isQueryable(columnName, dataType))
                            .maxLength(maxLength)
                            .defaultValue(defaultValue)
                            .build();

                    columns.add(column);
                }
            }
        } catch (SQLException e) {
            log.error("PostgreSQL获取表字段失败: {}", tableName, e);
            throw new RuntimeException("获取表字段失败: " + e.getMessage(), e);
        }

        return columns;
    }

    @Override
    protected String getTableComment(DataSource dataSource, String tableName) {
        String sql = """
                SELECT obj_description((? || '.' || ?)::regclass::oid, 'pg_class') as table_comment
                """;

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            String schema = getSchema(conn);
            ps.setString(1, StrUtil.isBlank(schema) ? "public" : schema);
            ps.setString(2, tableName);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("table_comment");
                }
            }
        } catch (SQLException e) {
            log.error("PostgreSQL获取表注释失败: {}", tableName, e);
        }

        return "";
    }

    @Override
    protected String getSchema(Connection conn) throws SQLException {
        String schema = conn.getSchema();
        return StrUtil.isBlank(schema) ? "public" : schema;
    }

    @Override
    protected String getDatabaseTypeName() {
        return "PostgreSQL";
    }

    /**
     * 规范化PostgreSQL数据类型
     * 将PostgreSQL特有类型映射为通用类型名称
     */
    private String normalizeDataType(String pgType) {
        return switch (pgType.toLowerCase()) {
            case "int2" -> "smallint";
            case "int4" -> "int";
            case "int8" -> "bigint";
            case "float4" -> "float";
            case "float8" -> "double";
            case "bool" -> "boolean";
            case "bpchar" -> "char";
            case "timestamptz" -> "timestamp";
            case "timetz" -> "time";
            default -> pgType;
        };
    }
}
