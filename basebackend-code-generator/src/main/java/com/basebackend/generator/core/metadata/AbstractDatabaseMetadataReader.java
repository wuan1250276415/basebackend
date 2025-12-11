package com.basebackend.generator.core.metadata;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据库元数据读取器抽象基类
 * 
 * 提取公共逻辑，减少代码重复，便于扩展新的数据库类型
 */
@Slf4j
public abstract class AbstractDatabaseMetadataReader implements DatabaseMetadataReader {

    /**
     * 系统字段集合（这些字段通常由框架管理）
     */
    protected static final Set<String> SYSTEM_FIELDS = Set.of(
            "id", "create_time", "update_time", "create_by", "update_by",
            "deleted", "created_at", "updated_at", "created_by", "updated_by");

    /**
     * 不可查询的数据类型（大文本类型）
     */
    protected static final Set<String> NON_QUERYABLE_TYPES = Set.of(
            "text", "longtext", "mediumtext", "clob", "blob", "bytea");

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
            if (isDateTimeType(column.getJavaType())) {
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
    public List<String> getPrimaryKeys(DataSource dataSource, String tableName) {
        List<String> primaryKeys = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = getCatalog(conn);
            String schema = getSchema(conn);

            try (ResultSet rs = metaData.getPrimaryKeys(catalog, schema, tableName)) {
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
     * 获取表注释（子类实现）
     */
    protected abstract String getTableComment(DataSource dataSource, String tableName);

    /**
     * 获取Catalog
     */
    protected String getCatalog(Connection conn) throws SQLException {
        return conn.getCatalog();
    }

    /**
     * 获取Schema
     */
    protected String getSchema(Connection conn) throws SQLException {
        return conn.getSchema();
    }

    /**
     * 判断是否是日期时间类型
     */
    protected boolean isDateTimeType(String javaType) {
        return javaType != null && (javaType.equals("LocalDateTime") ||
                javaType.equals("LocalDate") ||
                javaType.equals("LocalTime") ||
                javaType.equals("Date") ||
                javaType.equals("Timestamp"));
    }

    /**
     * 判断字段是否是系统字段
     */
    protected boolean isSystemField(String columnName) {
        return SYSTEM_FIELDS.contains(columnName.toLowerCase());
    }

    /**
     * 判断字段是否可查询
     */
    protected boolean isQueryable(String columnName, String dataType) {
        return !isSystemField(columnName) && !NON_QUERYABLE_TYPES.contains(dataType.toLowerCase());
    }

    /**
     * 获取数据库类型名称（用于日志）
     */
    protected abstract String getDatabaseTypeName();
}
