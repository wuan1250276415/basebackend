package com.basebackend.feign.client.scheduler;

import com.basebackend.common.model.Result;
import com.basebackend.feign.dto.scheduler.FormTemplateFeignDTO;
import com.basebackend.feign.fallback.scheduler.FormTemplateFeignFallbackFactory;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 表单模板 Feign 客户端
 * 提供表单模板查询和管理接口，供其他服务调用
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@FeignClient(
        name = "basebackend-scheduler",
        contextId = "formTemplateFeignClient",
        path = "/api/camunda/form-templates",
        fallbackFactory = FormTemplateFeignFallbackFactory.class
)
public interface FormTemplateFeignClient {

    /**
     * 根据ID获取表单模板详情
     *
     * @param formId 表单ID
     * @return 表单模板详情
     */
    @GetMapping("/{formId}")
    Result<FormTemplateFeignDTO> getById(
            @Parameter(description = "表单ID") @PathVariable("formId") Long formId
    );

    /**
     * 获取表单模板列表
     *
     * @param name 表单名称（可选）
     * @param code 表单编码（可选）
     * @param category 表单分类（可选）
     * @param status 状态（可选：draft、published、deprecated）
     * @param businessType 业务类型（可选）
     * @param processDefinitionKey 关联的流程定义键（可选）
     * @param enabled 是否启用（可选，默认true）
     * @param limit 返回数量限制（可选，默认100）
     * @return 表单模板列表
     */
    @GetMapping
    Result<List<FormTemplateFeignDTO>> getList(
            @Parameter(description = "表单名称") @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "表单编码") @RequestParam(value = "code", required = false) String code,
            @Parameter(description = "表单分类") @RequestParam(value = "category", required = false) String category,
            @Parameter(description = "状态") @RequestParam(value = "status", required = false) String status,
            @Parameter(description = "业务类型") @RequestParam(value = "businessType", required = false) String businessType,
            @Parameter(description = "关联的流程定义键") @RequestParam(value = "processDefinitionKey", required = false) String processDefinitionKey,
            @Parameter(description = "是否启用") @RequestParam(value = "enabled", defaultValue = "true") Boolean enabled,
            @Parameter(description = "返回数量限制") @RequestParam(value = "limit", defaultValue = "100") Integer limit
    );

    /**
     * 根据编码获取表单模板
     *
     * @param code 表单编码
     * @return 表单模板
     */
    @GetMapping("/by-code/{code}")
    Result<FormTemplateFeignDTO> getByCode(
            @Parameter(description = "表单编码") @PathVariable("code") String code
    );

    /**
     * 根据流程定义键获取表单模板
     *
     * @param processDefinitionKey 流程定义键
     * @param taskDefinitionKey 任务定义键（可选）
     * @return 表单模板
     */
    @GetMapping("/by-process-definition")
    Result<FormTemplateFeignDTO> getByProcessDefinitionKey(
            @Parameter(description = "流程定义键") @RequestParam("processDefinitionKey") String processDefinitionKey,
            @Parameter(description = "任务定义键") @RequestParam(value = "taskDefinitionKey", required = false) String taskDefinitionKey
    );

    /**
     * 根据业务类型获取表单模板
     *
     * @param businessType 业务类型
     * @param enabled 是否启用（可选，默认true）
     * @return 表单模板列表
     */
    @GetMapping("/by-business-type")
    Result<List<FormTemplateFeignDTO>> getByBusinessType(
            @Parameter(description = "业务类型") @RequestParam("businessType") String businessType,
            @Parameter(description = "是否启用") @RequestParam(value = "enabled", defaultValue = "true") Boolean enabled
    );

    /**
     * 检查表单编码是否唯一
     *
     * @param code 表单编码
     * @param formId 表单ID（可选，用于更新时排除自己）
     * @return 是否唯一
     */
    @GetMapping("/check-code-unique")
    Result<Boolean> checkCodeUnique(
            @Parameter(description = "表单编码") @RequestParam("code") String code,
            @Parameter(description = "表单ID") @RequestParam(value = "formId", required = false) Long formId
    );

    /**
     * 获取表单分类列表
     *
     * @param enabled 是否启用（可选，默认true）
     * @return 分类列表
     */
    @GetMapping("/categories")
    Result<List<String>> getCategories(
            @Parameter(description = "是否启用") @RequestParam(value = "enabled", defaultValue = "true") Boolean enabled
    );

    /**
     * 获取业务类型列表
     *
     * @param enabled 是否启用（可选，默认true）
     * @return 业务类型列表
     */
    @GetMapping("/business-types")
    Result<List<String>> getBusinessTypes(
            @Parameter(description = "是否启用") @RequestParam(value = "enabled", defaultValue = "true") Boolean enabled
    );

    /**
     * 获取表单模板统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/statistics")
    Result<Object> getStatistics();

    /**
     * 启用表单模板
     *
     * @param formId 表单ID
     * @return 操作结果
     */
    @PutMapping("/{formId}/enable")
    Result<Void> enable(
            @Parameter(description = "表单ID") @PathVariable("formId") Long formId
    );

    /**
     * 禁用表单模板
     *
     * @param formId 表单ID
     * @return 操作结果
     */
    @PutMapping("/{formId}/disable")
    Result<Void> disable(
            @Parameter(description = "表单ID") @PathVariable("formId") Long formId
    );

    /**
     * 获取启用的表单模板
     *
     * @param limit 返回数量限制（可选，默认100）
     * @return 表单模板列表
     */
    @GetMapping("/enabled")
    Result<List<FormTemplateFeignDTO>> getEnabledList(
            @Parameter(description = "返回数量限制") @RequestParam(value = "limit", defaultValue = "100") Integer limit
    );
}
