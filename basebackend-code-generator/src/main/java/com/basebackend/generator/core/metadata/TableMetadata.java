package com.basebackend.generator.core.metadata;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 表元数据
 */
@Data
@Builder
public class TableMetadata {

    /**
     * 表名
     */
    private String tableName;

    /**
     * 表注释
     */
    private String tableComment;

    /**
     * 类名（UpperCamelCase）
     */
    private String className;

    /**
     * 变量名（lowerCamelCase）
     */
    private String variableName;

    /**
     * URL路径（kebab-case）
     */
    private String urlPath;

    /**
     * 模块名
     */
    private String moduleName;

    /**
     * 包名
     */
    private String packageName;

    /**
     * 字段列表
     */
    private List<ColumnMetadata> columns;

    /**
     * 主键字段
     */
    private ColumnMetadata primaryKey;

    /**
     * 是否有日期时间类型
     */
    private Boolean hasDateTime;

    /**
     * 是否有BigDecimal类型
     */
    private Boolean hasBigDecimal;

    /**
     * 导入包列表
     */
    private List<String> importPackages;
}
