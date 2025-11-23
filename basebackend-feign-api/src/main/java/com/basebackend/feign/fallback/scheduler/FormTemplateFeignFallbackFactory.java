package com.basebackend.feign.fallback.scheduler;

import com.basebackend.common.model.Result;
import com.basebackend.feign.client.scheduler.FormTemplateFeignClient;
import com.basebackend.feign.dto.scheduler.FormTemplateFeignDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 表单模板 Feign 降级处理工厂
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Slf4j
@Component
public class FormTemplateFeignFallbackFactory implements FallbackFactory<FormTemplateFeignClient> {

    @Override
    public FormTemplateFeignClient create(Throwable cause) {
        log.error("[Feign降级] 调度器服务不可用: {}", cause.getMessage(), cause);

        return new FormTemplateFeignClient() {

            @Override
            public Result<FormTemplateFeignDTO> getById(Long formId) {
                log.error("[Feign降级] 根据ID查询表单模板失败: formId={}, error={}", formId, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<List<FormTemplateFeignDTO>> getList(String name, String code, String category, String status, String businessType, String processDefinitionKey, Boolean enabled, Integer limit) {
                log.error("[Feign降级] 查询表单模板列表失败: name={}, error={}", name, cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，返回空列表", Collections.emptyList());
            }

            @Override
            public Result<FormTemplateFeignDTO> getByCode(String code) {
                log.error("[Feign降级] 根据编码查询表单模板失败: code={}, error={}", code, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<FormTemplateFeignDTO> getByProcessDefinitionKey(String processDefinitionKey, String taskDefinitionKey) {
                log.error("[Feign降级] 根据流程定义键查询表单模板失败: processDefinitionKey={}, error={}", processDefinitionKey, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<List<FormTemplateFeignDTO>> getByBusinessType(String businessType, Boolean enabled) {
                log.error("[Feign降级] 根据业务类型查询表单模板失败: businessType={}, error={}", businessType, cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，返回空列表", Collections.emptyList());
            }

            @Override
            public Result<Boolean> checkCodeUnique(String code, Long formId) {
                log.error("[Feign降级] 检查表单编码唯一性失败: code={}, error={}", code, cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，建议稍后重试", false);
            }

            @Override
            public Result<List<String>> getCategories(Boolean enabled) {
                log.error("[Feign降级] 获取表单分类列表失败: error={}", cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，返回空列表", Collections.emptyList());
            }

            @Override
            public Result<List<String>> getBusinessTypes(Boolean enabled) {
                log.error("[Feign降级] 获取业务类型列表失败: error={}", cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，返回空列表", Collections.emptyList());
            }

            @Override
            public Result<Object> getStatistics() {
                log.error("[Feign降级] 获取表单模板统计失败: error={}", cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，返回空统计", Collections.emptyMap());
            }

            @Override
            public Result<Void> enable(Long formId) {
                log.error("[Feign降级] 启用表单模板失败: formId={}, error={}", formId, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<Void> disable(Long formId) {
                log.error("[Feign降级] 禁用表单模板失败: formId={}, error={}", formId, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<List<FormTemplateFeignDTO>> getEnabledList(Integer limit) {
                log.error("[Feign降级] 获取启用的表单模板列表失败: error={}", cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，返回空列表", Collections.emptyList());
            }
        };
    }
}
