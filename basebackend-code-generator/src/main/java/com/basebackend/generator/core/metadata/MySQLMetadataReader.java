package com.basebackend.generator.core.metadata;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MySQL元数据读取器
 */
@Slf4j
@Component
public class MySQLMetadataReader implements DatabaseMetadataReader {

    @Override
    public List<String> getTableNames(DataSource dataSource, String schema) {
        List<String> tables = new ArrayList<>();
        String sql = "SELECT TABLE_NAME FROM information_schema.TABLES " +
                     "WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE' " +
                     "ORDER BY TABLE_NAME";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
        } catch (SQLException e) {
            log.error("获取表列表失败", e);
            throw new RuntimeException("获取表列表失败: " + e.getMessage(), e);
        }

        return tables;
    }

    @Override
    public TableMetadata getTableMetadata(DataSource dataSource, String tableName) {
        // 获取表注释
        String tableComment = getTableComment(dataSource, tableName);
        
        // 获取列信息
        List<ColumnMetadata> columns = getColumns(dataSource, tableName);
        
        // 获取主键
        List<String> primaryKeys = getPrimaryKeys(dataSource, tableName);
        ColumnMetadata primaryKey = columns.stream()
                .filter(col -> primaryKeys.contains(col.getColumnName()))
                .findFirst()
                .orElse(null);

        // 分析导入包
        Set<String> imports = new HashSet<>();
        boolean hasDateTime = false;
        boolean hasBigDecimal = false;
        
        for (ColumnMetadata column : columns) {
            if (StrUtil.isNotBlank(column.getImportPackage())) {
                imports.add(column.getImportPackage());
            }
            if ("LocalDateTime".equals(column.getJavaType()) || 
                "LocalDate".equals(column.getJavaType()) || 
                "LocalTime".equals(column.getJavaType())) {
                hasDateTime = true;
            }
            if ("BigDecimal".equals(column.getJavaType())) {
                hasBigDecimal = true;
            }
        }

        return TableMetadata.builder()
                .tableName(tableName)
                .tableComment(StrUtil.isNotBlank(tableComment) ? tableComment : tableName)
                .columns(columns)
                .primaryKey(primaryKey)
                .hasDateTime(hasDateTime)
                .hasBigDecimal(hasBigDecimal)
                .importPackages(new ArrayList<>(imports))
                .build();
    }

    @Override
    public List<ColumnMetadata> getColumns(DataSource dataSource, String tableName) {
        List<ColumnMetadata> columns = new ArrayList<>();
        String sql = "SELECT COLUMN_NAME, COLUMN_TYPE, DATA_TYPE, COLUMN_COMMENT, " +
                     "IS_NULLABLE, COLUMN_KEY, EXTRA, COLUMN_DEFAULT, CHARACTER_MAXIMUM_LENGTH " +
                     "FROM information_schema.COLUMNS " +
                     "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? " +
                     "ORDER BY ORDINAL_POSITION";

        List<String> primaryKeys = getPrimaryKeys(dataSource, tableName);
        Set<String> systemFields = Set.of("id", "create_time", "update_time", "create_by", "update_by", "deleted");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    String dataType = rs.getString("DATA_TYPE").toLowerCase();
                    String columnComment = rs.getString("COLUMN_COMMENT");
                    boolean nullable = "YES".equals(rs.getString("IS_NULLABLE"));
                    boolean isPrimaryKey = primaryKeys.contains(columnName);
                    boolean isAutoIncrement = "auto_increment".equals(rs.getString("EXTRA"));
                    String defaultValue = rs.getString("COLUMN_DEFAULT");
                    Integer maxLength = rs.getInt("CHARACTER_MAXIMUM_LENGTH");

                    ColumnMetadata column = ColumnMetadata.builder()
                            .columnName(columnName)
                            .columnType(dataType)
                            .columnComment(StrUtil.isNotBlank(columnComment) ? columnComment : columnName)
                            .isPrimaryKey(isPrimaryKey)
                            .isAutoIncrement(isAutoIncrement)
                            .nullable(nullable)
                            .isSystemField(systemFields.contains(columnName))
                            .queryable(!systemFields.contains(columnName) && !"text".equals(dataType) && !"longtext".equals(dataType))
                            .maxLength(maxLength)
                            .defaultValue(defaultValue)
                            .build();

                    columns.add(column);
                }
            }
        } catch (SQLException e) {
            log.error("获取表字段失败: {}", tableName, e);
            throw new RuntimeException("获取表字段失败: " + e.getMessage(), e);
        }

        return columns;
    }

    @Override
    public List<String> getPrimaryKeys(DataSource dataSource, String tableName) {
        List<String> primaryKeys = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getPrimaryKeys(conn.getCatalog(), null, tableName)) {
                while (rs.next()) {
                    primaryKeys.add(rs.getString("COLUMN_NAME"));
                }
            }
        } catch (SQLException e) {
            log.error("获取主键失败: {}", tableName, e);
            throw new RuntimeException("获取主键失败: " + e.getMessage(), e);
        }

        return primaryKeys;
    }

    /**
     * 获取表注释
     */
    private String getTableComment(DataSource dataSource, String tableName) {
        String sql = "SELECT TABLE_COMMENT FROM information_schema.TABLES " +
                     "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("TABLE_COMMENT");
                }
            }
        } catch (SQLException e) {
            log.error("获取表注释失败: {}", tableName, e);
        }

        return "";
    }
}
