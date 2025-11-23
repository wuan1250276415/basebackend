package com.basebackend.feign.fallback.scheduler;

import com.basebackend.common.model.Result;
import com.basebackend.feign.client.scheduler.ProcessDefinitionFeignClient;
import com.basebackend.feign.dto.scheduler.ProcessDefinitionFeignDTO;
import com.basebackend.feign.dto.scheduler.ProcessDefinitionStartRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 流程定义 Feign 降级处理工厂
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Slf4j
@Component
public class ProcessDefinitionFeignFallbackFactory implements FallbackFactory<ProcessDefinitionFeignClient> {

    @Override
    public ProcessDefinitionFeignClient create(Throwable cause) {
        log.error("[Feign降级] 调度器服务不可用: {}", cause.getMessage(), cause);

        return new ProcessDefinitionFeignClient() {

            @Override
            public Result<ProcessDefinitionFeignDTO> getById(String processDefinitionId) {
                log.error("[Feign降级] 根据ID查询流程定义失败: processDefinitionId={}, error={}", processDefinitionId, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<ProcessDefinitionFeignDTO> getLatestVersion(String key, String tenantId) {
                log.error("[Feign降级] 获取流程定义最新版本失败: key={}, tenantId={}, error={}", key, tenantId, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<List<ProcessDefinitionFeignDTO>> getList(String key, String name, String tenantId, Integer version, Boolean latestVersion) {
                log.error("[Feign降级] 查询流程定义列表失败: key={}, error={}", key, cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，返回空列表", Collections.emptyList());
            }

            @Override
            public Result<String> startProcessInstance(ProcessDefinitionStartRequest request) {
                log.error("[Feign降级] 启动流程实例失败: processDefinitionKey={}, error={}", request.getProcessDefinitionKey(), cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，流程启动失败");
            }

            @Override
            public Result<String> startProcessInstanceByKey(String key, String businessKey, String variables, String tenantId) {
                log.error("[Feign降级] 根据流程定义键启动流程失败: key={}, error={}", key, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，流程启动失败");
            }

            @Override
            public Result<Void> activate(String processDefinitionId) {
                log.error("[Feign降级] 激活流程定义失败: processDefinitionId={}, error={}", processDefinitionId, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<Void> suspend(String processDefinitionId) {
                log.error("[Feign降级] 挂起流程定义失败: processDefinitionId={}, error={}", processDefinitionId, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<Boolean> checkExistence(String key, String tenantId) {
                log.error("[Feign降级] 检查流程定义存在性失败: key={}, error={}", key, cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，建议稍后重试", false);
            }

            @Override
            public Result<List<ProcessDefinitionFeignDTO>> getVersions(String key, String tenantId) {
                log.error("[Feign降级] 获取流程定义版本列表失败: key={}, error={}", key, cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，返回空列表", Collections.emptyList());
            }
        };
    }
}
