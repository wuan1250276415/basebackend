package com.basebackend.service.client.fallback.scheduler;

import com.basebackend.api.model.scheduler.FormTemplateFeignDTO;
import com.basebackend.common.model.Result;
import com.basebackend.service.client.scheduler.FormTemplateServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 表单模板服务客户端降级实现
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Component
public class FormTemplateServiceClientFallback implements FormTemplateServiceClient {

    private static final Logger log = LoggerFactory.getLogger(FormTemplateServiceClientFallback.class);

    @Override
    public Result<FormTemplateFeignDTO> getById(Long formId) {
        log.error("[服务降级] 获取表单模板失败: formId={}", formId);
        return Result.error("调度器服务不可用，获取表单模板失败");
    }

    @Override
    public Result<List<FormTemplateFeignDTO>> getList(String name, String code, String category,
            String status, String businessType, String processDefinitionKey, Boolean enabled, Integer limit) {
        log.error("[服务降级] 获取表单模板列表失败");
        return Result.success("调度器服务降级，返回空列表", Collections.emptyList());
    }

    @Override
    public Result<FormTemplateFeignDTO> getByCode(String code) {
        log.error("[服务降级] 根据编码获取表单模板失败: code={}", code);
        return Result.error("调度器服务不可用");
    }

    @Override
    public Result<FormTemplateFeignDTO> getByProcessDefinitionKey(String processDefinitionKey, String taskDefinitionKey) {
        log.error("[服务降级] 根据流程定义获取表单模板失败: key={}", processDefinitionKey);
        return Result.error("调度器服务不可用");
    }

    @Override
    public Result<List<FormTemplateFeignDTO>> getByBusinessType(String businessType, Boolean enabled) {
        log.error("[服务降级] 根据业务类型获取表单模板失败: businessType={}", businessType);
        return Result.success("调度器服务降级，返回空列表", Collections.emptyList());
    }

    @Override
    public Result<Boolean> checkCodeUnique(String code, Long formId) {
        log.error("[服务降级] 检查编码唯一性失败: code={}", code);
        return Result.success("调度器服务降级", false);
    }

    @Override
    public Result<List<String>> getCategories(Boolean enabled) {
        log.error("[服务降级] 获取分类列表失败");
        return Result.success("调度器服务降级，返回空列表", Collections.emptyList());
    }

    @Override
    public Result<List<String>> getBusinessTypes(Boolean enabled) {
        log.error("[服务降级] 获取业务类型列表失败");
        return Result.success("调度器服务降级，返回空列表", Collections.emptyList());
    }

    @Override
    public Result<Object> getStatistics() {
        log.error("[服务降级] 获取统计信息失败");
        return Result.success("调度器服务降级", Collections.emptyMap());
    }

    @Override
    public Result<Void> enable(Long formId) {
        log.error("[服务降级] 启用表单模板失败: formId={}", formId);
        return Result.error("调度器服务不可用");
    }

    @Override
    public Result<Void> disable(Long formId) {
        log.error("[服务降级] 禁用表单模板失败: formId={}", formId);
        return Result.error("调度器服务不可用");
    }

    @Override
    public Result<List<FormTemplateFeignDTO>> getEnabledList(Integer limit) {
        log.error("[服务降级] 获取启用的表单模板列表失败");
        return Result.success("调度器服务降级，返回空列表", Collections.emptyList());
    }
}
