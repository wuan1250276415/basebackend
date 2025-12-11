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
 * Oracle元数据读取器
 * 
 * 支持Oracle数据库的表结构读取和元数据提取
 * 兼容Oracle 11g及以上版本
 */
@Slf4j
@Component
public class OracleMetadataReader extends AbstractDatabaseMetadataReader {

    @Override
    public List<String> getTableNames(DataSource dataSource, String schema) {
        List<String> tables = new ArrayList<>();

        // 使用用户表视图获取表列表
        String sql = """
                SELECT table_name
                FROM all_tables
                WHERE owner = UPPER(NVL(?, USER))
                AND table_name NOT LIKE 'BIN$%'
                ORDER BY table_name
                """;

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
        } catch (SQLException e) {
            log.error("Oracle获取表列表失败", e);
            throw new RuntimeException("获取表列表失败: " + e.getMessage(), e);
        }

        return tables;
    }

    @Override
    public List<ColumnMetadata> getColumns(DataSource dataSource, String tableName) {
        List<ColumnMetadata> columns = new ArrayList<>();

        // Oracle列信息查询，包含注释和主键信息
        String sql = """
                SELECT
                    c.column_name,
                    c.data_type,
                    c.nullable,
                    c.data_default,
                    c.char_length,
                    c.data_precision,
                    c.data_scale,
                    NVL(cc.comments, c.column_name) as column_comment,
                    CASE WHEN pk.column_name IS NOT NULL THEN 'Y' ELSE 'N' END as is_primary_key,
                    CASE WHEN c.data_default LIKE '%.nextval%' THEN 'Y' ELSE 'N' END as is_sequence
                FROM all_tab_columns c
                LEFT JOIN all_col_comments cc
                    ON c.owner = cc.owner
                    AND c.table_name = cc.table_name
                    AND c.column_name = cc.column_name
                LEFT JOIN (
                    SELECT acc.column_name
                    FROM all_constraints ac
                    JOIN all_cons_columns acc ON ac.constraint_name = acc.constraint_name
                    WHERE ac.constraint_type = 'P'
                    AND ac.table_name = UPPER(?)
                ) pk ON c.column_name = pk.column_name
                WHERE c.table_name = UPPER(?)
                ORDER BY c.column_id
                """;

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tableName);
            ps.setString(2, tableName);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    String dataType = rs.getString("DATA_TYPE").toLowerCase();
                    String columnComment = rs.getString("COLUMN_COMMENT");
                    boolean nullable = "Y".equals(rs.getString("NULLABLE"));
                    boolean isPrimaryKey = "Y".equals(rs.getString("IS_PRIMARY_KEY"));
                    boolean isAutoIncrement = "Y".equals(rs.getString("IS_SEQUENCE"));
                    String defaultValue = rs.getString("DATA_DEFAULT");
                    Integer maxLength = rs.getObject("CHAR_LENGTH") != null
                            ? rs.getInt("CHAR_LENGTH")
                            : null;

                    ColumnMetadata column = ColumnMetadata.builder()
                            .columnName(columnName.toLowerCase()) // Oracle默认大写，转为小写
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
            log.error("Oracle获取表字段失败: {}", tableName, e);
            throw new RuntimeException("获取表字段失败: " + e.getMessage(), e);
        }

        return columns;
    }

    @Override
    public List<String> getPrimaryKeys(DataSource dataSource, String tableName) {
        List<String> primaryKeys = new ArrayList<>();

        String sql = """
                SELECT acc.column_name
                FROM all_constraints ac
                JOIN all_cons_columns acc ON ac.constraint_name = acc.constraint_name
                WHERE ac.constraint_type = 'P'
                AND ac.table_name = UPPER(?)
                ORDER BY acc.position
                """;

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    primaryKeys.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }
        } catch (SQLException e) {
            log.error("Oracle获取主键失败: {}", tableName, e);
            throw new RuntimeException("获取主键失败: " + e.getMessage(), e);
        }

        return primaryKeys;
    }

    @Override
    protected String getTableComment(DataSource dataSource, String tableName) {
        String sql = """
                SELECT comments
                FROM all_tab_comments
                WHERE table_name = UPPER(?)
                AND owner = USER
                """;

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("COMMENTS");
                }
            }
        } catch (SQLException e) {
            log.error("Oracle获取表注释失败: {}", tableName, e);
        }

        return "";
    }

    @Override
    protected String getDatabaseTypeName() {
        return "Oracle";
    }

    /**
     * 规范化Oracle数据类型
     * 将Oracle特有类型映射为通用类型名称
     */
    private String normalizeDataType(String oracleType) {
        String type = oracleType.toLowerCase();

        // 处理带精度的类型
        if (type.startsWith("number")) {
            return "number";
        }
        if (type.startsWith("varchar2")) {
            return "varchar";
        }
        if (type.startsWith("nvarchar2")) {
            return "nvarchar";
        }
        if (type.startsWith("char")) {
            return "char";
        }
        if (type.startsWith("nchar")) {
            return "nchar";
        }
        if (type.startsWith("timestamp")) {
            return "timestamp";
        }

        return switch (type) {
            case "integer" -> "int";
            case "binary_float" -> "float";
            case "binary_double" -> "double";
            case "long" -> "text";
            case "raw", "long raw" -> "blob";
            case "xmltype" -> "text";
            default -> type;
        };
    }
}
