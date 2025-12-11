package com.basebackend.scheduler.camunda.controller;

import com.basebackend.common.dto.PageResult;
import com.basebackend.common.model.Result;
import com.basebackend.scheduler.camunda.dto.FormTemplateCreateRequest;
import com.basebackend.scheduler.camunda.dto.FormTemplateDTO;
import com.basebackend.scheduler.camunda.dto.FormTemplatePageQuery;
import com.basebackend.scheduler.camunda.dto.FormTemplateUpdateRequest;
import com.basebackend.scheduler.camunda.service.FormTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Camunda 表单模板控制器
 *
 * <p>提供表单模板的 CRUD 管理功能，包括：
 * <ul>
 *   <li>表单模板分页查询（按租户、类型、状态过滤）</li>
 *   <li>表单模板详情查看</li>
 *   <li>表单模板创建</li>
 *   <li>表单模板更新</li>
 *   <li>表单模板删除</li>
 * </ul>
 *
 * <p>设计原则：
 * <ul>
 *   <li>RESTful API 设计，遵循标准 HTTP 方法语义</li>
 *   <li>完善的参数验证和错误处理机制</li>
 *   <li>支持租户隔离和多租户场景</li>
 *   <li>支持表单模板版本管理</li>
 *   <li>详细的审计日志记录</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/camunda/form-templates")
@RequiredArgsConstructor
@Tag(name = "Camunda 表单模板管理", description = "表单模板 CRUD 管理 API")
@SecurityRequirement(name = "BearerAuth")
public class FormTemplateController {

    private final FormTemplateService formTemplateService;

    /**
     * 分页查询表单模板
     *
     * <p>支持多种过滤条件：
     * <ul>
     *   <li>租户过滤</li>
     *   <li>表单类型过滤</li>
     *   <li>表单状态过滤</li>
     *   <li>关键词模糊搜索（名称、描述）</li>
     * </ul>
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    @Operation(
        summary = "分页查询表单模板",
        description = "支持租户、类型、状态过滤的表单模板分页查询"
    )
    @GetMapping
    public Result<PageResult<FormTemplateDTO>> page(@ParameterObject @Valid FormTemplatePageQuery query) {
        PageResult<FormTemplateDTO> result = formTemplateService.page(query);
        return Result.success(result);
    }

    /**
     * 获取表单模板详情
     *
     * @param templateId 模板 ID
     * @return 表单模板详情
     */
    @Operation(
        summary = "获取表单模板详情",
        description = "根据模板 ID 获取表单模板的详细信息"
    )
    @GetMapping("/{templateId}")
    public Result<FormTemplateDTO> detail(
            @Parameter(description = "模板 ID") @PathVariable @NotNull @Positive Long templateId) {
        FormTemplateDTO dto = formTemplateService.detail(templateId);
        return Result.success(dto);
    }

    /**
     * 创建表单模板
     *
     * @param request 创建请求参数
     * @return 创建结果
     */
    @Operation(
        summary = "创建表单模板",
        description = "创建新的表单模板，支持多种表单类型"
    )
    @PostMapping
    public Result<FormTemplateDTO> create(@Valid @RequestBody FormTemplateCreateRequest request) {
        FormTemplateDTO dto = formTemplateService.create(request);
        return Result.success("创建成功", dto);
    }

    /**
     * 更新表单模板
     *
     * @param templateId 模板 ID
     * @param request 更新请求参数
     * @return 更新结果
     */
    @Operation(
        summary = "更新表单模板",
        description = "更新表单模板信息，自动增加版本号"
    )
    @PutMapping("/{templateId}")
    public Result<FormTemplateDTO> update(
            @Parameter(description = "模板 ID") @PathVariable @NotNull @Positive Long templateId,
            @Valid @RequestBody FormTemplateUpdateRequest request) {
        FormTemplateDTO dto = formTemplateService.update(templateId, request);
        return Result.success("更新成功", dto);
    }

    /**
     * 删除表单模板
     *
     * @param templateId 模板 ID
     * @return 删除结果
     */
    @Operation(
        summary = "删除表单模板",
        description = "删除指定的表单模板（软删除）"
    )
    @DeleteMapping("/{templateId}")
    public Result<String> delete(
            @Parameter(description = "模板 ID") @PathVariable @NotNull @Positive Long templateId) {
        formTemplateService.delete(templateId);
        return Result.success("删除成功");
    }
}
