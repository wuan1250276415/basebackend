package com.basebackend.service.client.fallback.scheduler;

import com.basebackend.api.model.scheduler.TaskActionRequest;
import com.basebackend.api.model.scheduler.TaskFeignDTO;
import com.basebackend.common.model.Result;
import com.basebackend.service.client.scheduler.TaskServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 任务服务客户端降级实现
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Component
public class TaskServiceClientFallback implements TaskServiceClient {

    private static final Logger log = LoggerFactory.getLogger(TaskServiceClientFallback.class);

    @Override
    public Result<TaskFeignDTO> getById(String taskId, Boolean withVariables) {
        log.error("[服务降级] 获取任务失败: taskId={}", taskId);
        return Result.error("调度器服务不可用，获取任务失败");
    }

    @Override
    public Result<List<TaskFeignDTO>> getList(String assignee, String candidateUser, String candidateGroup,
            String processInstanceId, String processDefinitionKey, String name, String state,
            String tenantId, Integer limit) {
        log.error("[服务降级] 获取任务列表失败");
        return Result.success("调度器服务降级，返回空列表", Collections.emptyList());
    }

    @Override
    public Result<Void> claim(String taskId, String userId) {
        log.error("[服务降级] 认领任务失败: taskId={}", taskId);
        return Result.error("调度器服务不可用");
    }

    @Override
    public Result<Void> unclaim(String taskId, String userId) {
        log.error("[服务降级] 取消认领任务失败: taskId={}", taskId);
        return Result.error("调度器服务不可用");
    }

    @Override
    public Result<Void> complete(String taskId, TaskActionRequest request) {
        log.error("[服务降级] 完成任务失败: taskId={}", taskId);
        return Result.error("调度器服务不可用");
    }

    @Override
    public Result<Void> delegate(String taskId, String userId, String delegateUser) {
        log.error("[服务降级] 委派任务失败: taskId={}", taskId);
        return Result.error("调度器服务不可用");
    }

    @Override
    public Result<Object> getVariable(String taskId, String variableName) {
        log.error("[服务降级] 获取任务变量失败: taskId={}, variable={}", taskId, variableName);
        return Result.success("调度器服务降级", null);
    }

    @Override
    public Result<Map<String, Object>> getVariables(String taskId) {
        log.error("[服务降级] 获取所有任务变量失败: taskId={}", taskId);
        return Result.success("调度器服务降级，返回空Map", Collections.emptyMap());
    }

    @Override
    public Result<Void> setVariable(String taskId, String variableName, Object value) {
        log.error("[服务降级] 设置任务变量失败: taskId={}, variable={}", taskId, variableName);
        return Result.error("调度器服务不可用");
    }

    @Override
    public Result<Void> removeVariable(String taskId, String variableName) {
        log.error("[服务降级] 删除任务变量失败: taskId={}, variable={}", taskId, variableName);
        return Result.error("调度器服务不可用");
    }

    @Override
    public Result<Boolean> checkExistence(String taskId) {
        log.error("[服务降级] 检查任务是否存在失败: taskId={}", taskId);
        return Result.success("调度器服务降级", false);
    }

    @Override
    public Result<Long> getPendingTaskCount(String userId, String tenantId) {
        log.error("[服务降级] 获取待处理任务数失败: userId={}", userId);
        return Result.success("调度器服务降级", 0L);
    }

    @Override
    public Result<Map<String, Object>> getStatistics(String userId, String tenantId) {
        log.error("[服务降级] 获取任务统计失败: userId={}", userId);
        return Result.success("调度器服务降级，返回空Map", Collections.emptyMap());
    }

    @Override
    public Result<List<TaskFeignDTO>> getActiveTasksByProcessInstance(String processInstanceId) {
        log.error("[服务降级] 获取流程实例的活跃任务失败: processInstanceId={}", processInstanceId);
        return Result.success("调度器服务降级，返回空列表", Collections.emptyList());
    }
}
