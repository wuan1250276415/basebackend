package com.basebackend.service.client.fallback.scheduler;

import com.basebackend.api.model.scheduler.ProcessInstanceFeignDTO;
import com.basebackend.common.model.Result;
import com.basebackend.service.client.scheduler.ProcessInstanceServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 流程实例服务客户端降级实现
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Component
public class ProcessInstanceServiceClientFallback implements ProcessInstanceServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ProcessInstanceServiceClientFallback.class);

    @Override
    public Result<ProcessInstanceFeignDTO> getById(String instanceId, Boolean withVariables) {
        log.error("[服务降级] 获取流程实例失败: id={}", instanceId);
        return Result.error("调度器服务不可用，获取流程实例失败");
    }

    @Override
    public Result<List<ProcessInstanceFeignDTO>> getList(String processDefinitionId,
            String processDefinitionKey, String businessKey, String state, String tenantId, Integer limit) {
        log.error("[服务降级] 获取流程实例列表失败");
        return Result.success("调度器服务降级，返回空列表", Collections.emptyList());
    }

    @Override
    public Result<List<ProcessInstanceFeignDTO>> getByBusinessKey(String businessKey,
            String processDefinitionKey, String tenantId) {
        log.error("[服务降级] 根据业务键获取流程实例失败: businessKey={}", businessKey);
        return Result.success("调度器服务降级，返回空列表", Collections.emptyList());
    }

    @Override
    public Result<Void> activate(String instanceId) {
        log.error("[服务降级] 激活流程实例失败: id={}", instanceId);
        return Result.error("调度器服务不可用");
    }

    @Override
    public Result<Void> suspend(String instanceId) {
        log.error("[服务降级] 挂起流程实例失败: id={}", instanceId);
        return Result.error("调度器服务不可用");
    }

    @Override
    public Result<Void> delete(String instanceId, String reason) {
        log.error("[服务降级] 删除流程实例失败: id={}", instanceId);
        return Result.error("调度器服务不可用");
    }

    @Override
    public Result<Object> getVariable(String instanceId, String variableName) {
        log.error("[服务降级] 获取流程变量失败: instanceId={}, variable={}", instanceId, variableName);
        return Result.success("调度器服务降级", null);
    }

    @Override
    public Result<Map<String, Object>> getVariables(String instanceId) {
        log.error("[服务降级] 获取所有流程变量失败: instanceId={}", instanceId);
        return Result.success("调度器服务降级，返回空Map", Collections.emptyMap());
    }

    @Override
    public Result<Void> setVariable(String instanceId, String variableName, Object value) {
        log.error("[服务降级] 设置流程变量失败: instanceId={}, variable={}", instanceId, variableName);
        return Result.error("调度器服务不可用");
    }

    @Override
    public Result<Void> setVariables(String instanceId, Map<String, Object> variables) {
        log.error("[服务降级] 批量设置流程变量失败: instanceId={}", instanceId);
        return Result.error("调度器服务不可用");
    }

    @Override
    public Result<Void> removeVariable(String instanceId, String variableName) {
        log.error("[服务降级] 删除流程变量失败: instanceId={}, variable={}", instanceId, variableName);
        return Result.error("调度器服务不可用");
    }

    @Override
    public Result<Boolean> checkExistence(String instanceId) {
        log.error("[服务降级] 检查流程实例是否存在失败: id={}", instanceId);
        return Result.success("调度器服务降级", false);
    }

    @Override
    public Result<Map<String, Object>> getStatistics(String processDefinitionKey, String tenantId) {
        log.error("[服务降级] 获取统计信息失败");
        return Result.success("调度器服务降级，返回空Map", Collections.emptyMap());
    }
}
