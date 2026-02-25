package com.basebackend.generator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.Map;

/**
 * 代码生成请求DTO
 *
 * 添加参数验证注解，确保输入数据的有效性
 */
public record GenerateRequest(
    @NotNull(message = "数据源ID不能为空")
    Long datasourceId,
    @NotEmpty(message = "表名列表不能为空")
    List<@NotBlank(message = "表名不能为空白") String> tableNames,
    @NotNull(message = "模板分组ID不能为空")
    Long templateGroupId,
    @NotBlank(message = "生成类型不能为空")
    @Pattern(regexp = "^(DOWNLOAD|PREVIEW|INCREMENT)$", message = "生成类型必须是DOWNLOAD、PREVIEW或INCREMENT")
    String generateType,
    @NotBlank(message = "包名不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$", message = "包名格式不正确，必须符合Java包名规范")
    String packageName,
    @NotBlank(message = "模块名不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_-]*$", message = "模块名格式不正确")
    String moduleName,
    String author,
    String tablePrefix,
    Map<String, Object> config
) {}
