package com.basebackend.generator.core.metadata;

import lombok.Builder;
import lombok.Data;

/**
 * 字段元数据
 */
@Data
@Builder
public class ColumnMetadata {

    /**
     * 字段名
     */
    private String columnName;

    /**
     * 字段类型
     */
    private String columnType;

    /**
     * 字段注释
     */
    private String columnComment;

    /**
     * 是否主键
     */
    private Boolean isPrimaryKey;

    /**
     * 是否自增
     */
    private Boolean isAutoIncrement;

    /**
     * 是否可为空
     */
    private Boolean nullable;

    /**
     * Java字段名（lowerCamelCase）
     */
    private String javaField;

    /**
     * Java类型
     */
    private String javaType;

    /**
     * TypeScript类型
     */
    private String tsType;

    /**
     * 导入包
     */
    private String importPackage;

    /**
     * 是否系统字段（id/createTime/updateTime等）
     */
    private Boolean isSystemField;

    /**
     * 是否可查询
     */
    private Boolean queryable;

    /**
     * 最大长度
     */
    private Integer maxLength;

    /**
     * 默认值
     */
    private String defaultValue;
}
