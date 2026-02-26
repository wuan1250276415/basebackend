package com.basebackend.service.client.scheduler;

import com.basebackend.api.model.scheduler.FormTemplateFeignDTO;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * 表单模板服务客户端
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@HttpExchange("/api/camunda/form-templates")
public interface FormTemplateServiceClient {

    @GetExchange("/{formId}")
    @Operation(summary = "根据ID获取表单模板")
    Result<FormTemplateFeignDTO> getById(@PathVariable("formId") Long formId);

    @GetExchange
    @Operation(summary = "获取表单模板列表")
    Result<List<FormTemplateFeignDTO>> getList(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "businessType", required = false) String businessType,
            @RequestParam(value = "processDefinitionKey", required = false) String processDefinitionKey,
            @RequestParam(value = "enabled", required = false) Boolean enabled,
            @RequestParam(value = "limit", required = false) Integer limit);

    @GetExchange("/by-code")
    @Operation(summary = "根据编码获取表单模板")
    Result<FormTemplateFeignDTO> getByCode(@RequestParam("code") String code);

    @GetExchange("/by-process")
    @Operation(summary = "根据流程定义获取表单模板")
    Result<FormTemplateFeignDTO> getByProcessDefinitionKey(
            @RequestParam("processDefinitionKey") String processDefinitionKey,
            @RequestParam(value = "taskDefinitionKey", required = false) String taskDefinitionKey);

    @GetExchange("/by-business-type")
    @Operation(summary = "根据业务类型获取表单模板")
    Result<List<FormTemplateFeignDTO>> getByBusinessType(
            @RequestParam("businessType") String businessType,
            @RequestParam(value = "enabled", required = false) Boolean enabled);

    @GetExchange("/check-code")
    @Operation(summary = "检查编码唯一性")
    Result<Boolean> checkCodeUnique(@RequestParam("code") String code,
                                     @RequestParam(value = "formId", required = false) Long formId);

    @GetExchange("/categories")
    @Operation(summary = "获取分类列表")
    Result<List<String>> getCategories(@RequestParam(value = "enabled", required = false) Boolean enabled);

    @GetExchange("/business-types")
    @Operation(summary = "获取业务类型列表")
    Result<List<String>> getBusinessTypes(@RequestParam(value = "enabled", required = false) Boolean enabled);

    @GetExchange("/statistics")
    @Operation(summary = "获取统计信息")
    Result<Object> getStatistics();

    @PostExchange("/{formId}/enable")
    @Operation(summary = "启用表单模板")
    Result<Void> enable(@PathVariable("formId") Long formId);

    @PostExchange("/{formId}/disable")
    @Operation(summary = "禁用表单模板")
    Result<Void> disable(@PathVariable("formId") Long formId);

    @GetExchange("/enabled")
    @Operation(summary = "获取启用的表单模板列表")
    Result<List<FormTemplateFeignDTO>> getEnabledList(@RequestParam(value = "limit", required = false) Integer limit);
}
