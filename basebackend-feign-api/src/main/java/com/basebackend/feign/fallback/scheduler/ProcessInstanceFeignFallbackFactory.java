package com.basebackend.feign.fallback.scheduler;

import com.basebackend.common.model.Result;
import com.basebackend.feign.client.scheduler.ProcessInstanceFeignClient;
import com.basebackend.feign.dto.scheduler.ProcessInstanceFeignDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 流程实例 Feign 降级处理工厂
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Slf4j
@Component
public class ProcessInstanceFeignFallbackFactory implements FallbackFactory<ProcessInstanceFeignClient> {

    @Override
    public ProcessInstanceFeignClient create(Throwable cause) {
        log.error("[Feign降级] 调度器服务不可用: {}", cause.getMessage(), cause);

        return new ProcessInstanceFeignClient() {

            @Override
            public Result<ProcessInstanceFeignDTO> getById(String instanceId, Boolean withVariables) {
                log.error("[Feign降级] 根据ID查询流程实例失败: instanceId={}, error={}", instanceId, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<List<ProcessInstanceFeignDTO>> getList(String processDefinitionId, String processDefinitionKey, String businessKey, String state, String tenantId, Integer limit) {
                log.error("[Feign降级] 查询流程实例列表失败: processDefinitionKey={}, error={}", processDefinitionKey, cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，返回空列表", Collections.emptyList());
            }

            @Override
            public Result<List<ProcessInstanceFeignDTO>> getByBusinessKey(String businessKey, String processDefinitionKey, String tenantId) {
                log.error("[Feign降级] 根据业务键查询流程实例失败: businessKey={}, error={}", businessKey, cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，返回空列表", Collections.emptyList());
            }

            @Override
            public Result<Void> activate(String instanceId) {
                log.error("[Feign降级] 激活流程实例失败: instanceId={}, error={}", instanceId, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<Void> suspend(String instanceId) {
                log.error("[Feign降级] 挂起流程实例失败: instanceId={}, error={}", instanceId, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<Void> delete(String instanceId, String reason) {
                log.error("[Feign降级] 删除流程实例失败: instanceId={}, error={}", instanceId, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<Object> getVariable(String instanceId, String variableName) {
                log.error("[Feign降级] 获取流程变量失败: instanceId={}, variableName={}, error={}", instanceId, variableName, cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，返回空", null);
            }

            @Override
            public Result<Map<String, Object>> getVariables(String instanceId) {
                log.error("[Feign降级] 获取所有流程变量失败: instanceId={}, error={}", instanceId, cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，返回空Map", Collections.emptyMap());
            }

            @Override
            public Result<Void> setVariable(String instanceId, String variableName, Object value) {
                log.error("[Feign降级] 设置流程变量失败: instanceId={}, variableName={}, error={}", instanceId, variableName, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<Void> setVariables(String instanceId, Map<String, Object> variables) {
                log.error("[Feign降级] 批量设置流程变量失败: instanceId={}, error={}", instanceId, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<Void> removeVariable(String instanceId, String variableName) {
                log.error("[Feign降级] 删除流程变量失败: instanceId={}, variableName={}, error={}", instanceId, variableName, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<Boolean> checkExistence(String instanceId) {
                log.error("[Feign降级] 检查流程实例存在性失败: instanceId={}, error={}", instanceId, cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，建议稍后重试", false);
            }

            @Override
            public Result<Map<String, Object>> getStatistics(String processDefinitionKey, String tenantId) {
                log.error("[Feign降级] 获取流程实例统计失败: processDefinitionKey={}, error={}", processDefinitionKey, cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，返回空统计", Collections.emptyMap());
            }
        };
    }
}
