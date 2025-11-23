package com.basebackend.feign.fallback.scheduler;

import com.basebackend.common.model.Result;
import com.basebackend.feign.client.scheduler.TaskFeignClient;
import com.basebackend.feign.dto.scheduler.TaskActionRequest;
import com.basebackend.feign.dto.scheduler.TaskFeignDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 任务 Feign 降级处理工厂
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Slf4j
@Component
public class TaskFeignFallbackFactory implements FallbackFactory<TaskFeignClient> {

    @Override
    public TaskFeignClient create(Throwable cause) {
        log.error("[Feign降级] 调度器服务不可用: {}", cause.getMessage(), cause);

        return new TaskFeignClient() {

            @Override
            public Result<TaskFeignDTO> getById(String taskId, Boolean withVariables) {
                log.error("[Feign降级] 根据ID查询任务失败: taskId={}, error={}", taskId, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<List<TaskFeignDTO>> getList(String assignee, String candidateUser, String candidateGroup, String processInstanceId, String processDefinitionKey, String name, String state, String tenantId, Integer limit) {
                log.error("[Feign降级] 查询任务列表失败: assignee={}, error={}", assignee, cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，返回空列表", Collections.emptyList());
            }

            @Override
            public Result<Void> claim(String taskId, String userId) {
                log.error("[Feign降级] 认领任务失败: taskId={}, userId={}, error={}", taskId, userId, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<Void> unclaim(String taskId, String userId) {
                log.error("[Feign降级] 释放任务失败: taskId={}, userId={}, error={}", taskId, userId, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<Void> complete(String taskId, TaskActionRequest request) {
                log.error("[Feign降级] 完成任务失败: taskId={}, error={}", taskId, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<Void> delegate(String taskId, String userId, String delegateUser) {
                log.error("[Feign降级] 委派任务失败: taskId={}, userId={}, error={}", taskId, userId, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<Object> getVariable(String taskId, String variableName) {
                log.error("[Feign降级] 获取任务变量失败: taskId={}, variableName={}, error={}", taskId, variableName, cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，返回空", null);
            }

            @Override
            public Result<Map<String, Object>> getVariables(String taskId) {
                log.error("[Feign降级] 获取所有任务变量失败: taskId={}, error={}", taskId, cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，返回空Map", Collections.emptyMap());
            }

            @Override
            public Result<Void> setVariable(String taskId, String variableName, Object value) {
                log.error("[Feign降级] 设置任务变量失败: taskId={}, variableName={}, error={}", taskId, variableName, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<Void> removeVariable(String taskId, String variableName) {
                log.error("[Feign降级] 删除任务变量失败: taskId={}, variableName={}, error={}", taskId, variableName, cause.getMessage(), cause);
                return Result.error("调度器服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<Boolean> checkExistence(String taskId) {
                log.error("[Feign降级] 检查任务存在性失败: taskId={}, error={}", taskId, cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，建议稍后重试", false);
            }

            @Override
            public Result<Long> getPendingTaskCount(String userId, String tenantId) {
                log.error("[Feign降级] 获取用户待办任务数量失败: userId={}, error={}", userId, cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，返回0", 0L);
            }

            @Override
            public Result<Map<String, Object>> getStatistics(String userId, String tenantId) {
                log.error("[Feign降级] 获取任务统计失败: userId={}, error={}", userId, cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，返回空统计", Collections.emptyMap());
            }

            @Override
            public Result<List<TaskFeignDTO>> getActiveTasksByProcessInstance(String processInstanceId) {
                log.error("[Feign降级] 获取流程实例的活动任务失败: processInstanceId={}, error={}", processInstanceId, cause.getMessage(), cause);
                return Result.success("调度器服务暂时不可用，返回空列表", Collections.emptyList());
            }
        };
    }
}
