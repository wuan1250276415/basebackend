package com.basebackend.generator.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 代码生成请求DTO
 */
@Data
public class GenerateRequest {

    /**
     * 数据源ID
     */
    private Long datasourceId;

    /**
     * 表名列表
     */
    private List<String> tableNames;

    /**
     * 模板分组ID
     */
    private Long templateGroupId;

    /**
     * 生成类型：DOWNLOAD/PREVIEW/INCREMENT
     */
    private String generateType;

    /**
     * 包名
     */
    private String packageName;

    /**
     * 模块名
     */
    private String moduleName;

    /**
     * 作者
     */
    private String author;

    /**
     * 表前缀（生成时去除）
     */
    private String tablePrefix;

    /**
     * 其他配置
     */
    private Map<String, Object> config;
}
