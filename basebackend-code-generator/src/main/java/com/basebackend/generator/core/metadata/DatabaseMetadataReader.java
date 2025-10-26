package com.basebackend.generator.core.metadata;

import javax.sql.DataSource;
import java.util.List;

/**
 * 数据库元数据读取器接口
 */
public interface DatabaseMetadataReader {

    /**
     * 获取数据库所有表
     *
     * @param dataSource 数据源
     * @param schema     数据库schema
     * @return 表列表
     */
    List<String> getTableNames(DataSource dataSource, String schema);

    /**
     * 获取表的元数据
     *
     * @param dataSource 数据源
     * @param tableName  表名
     * @return 表元数据
     */
    TableMetadata getTableMetadata(DataSource dataSource, String tableName);

    /**
     * 获取表的所有列
     *
     * @param dataSource 数据源
     * @param tableName  表名
     * @return 列列表
     */
    List<ColumnMetadata> getColumns(DataSource dataSource, String tableName);

    /**
     * 获取表的主键字段
     *
     * @param dataSource 数据源
     * @param tableName  表名
     * @return 主键字段列表
     */
    List<String> getPrimaryKeys(DataSource dataSource, String tableName);
}
