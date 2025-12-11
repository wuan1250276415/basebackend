package com.basebackend.generator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 代码生成请求DTO
 * 
 * 添加参数验证注解，确保输入数据的有效性
 */
@Data
public class GenerateRequest {

    /**
     * 数据源ID
     */
    @NotNull(message = "数据源ID不能为空")
    private Long datasourceId;

    /**
     * 表名列表
     */
    @NotEmpty(message = "表名列表不能为空")
    private List<@NotBlank(message = "表名不能为空白") String> tableNames;

    /**
     * 模板分组ID
     */
    @NotNull(message = "模板分组ID不能为空")
    private Long templateGroupId;

    /**
     * 生成类型：DOWNLOAD/PREVIEW/INCREMENT
     */
    @NotBlank(message = "生成类型不能为空")
    @Pattern(regexp = "^(DOWNLOAD|PREVIEW|INCREMENT)$", message = "生成类型必须是DOWNLOAD、PREVIEW或INCREMENT")
    private String generateType;

    /**
     * 包名（符合Java包名规范）
     */
    @NotBlank(message = "包名不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$", message = "包名格式不正确，必须符合Java包名规范")
    private String packageName;

    /**
     * 模块名
     */
    @NotBlank(message = "模块名不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_-]*$", message = "模块名格式不正确")
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
