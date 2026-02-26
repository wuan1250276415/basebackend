package com.basebackend.service.client.scheduler;

import com.basebackend.api.model.scheduler.TaskActionRequest;
import com.basebackend.api.model.scheduler.TaskFeignDTO;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import java.util.List;
import java.util.Map;

/**
 * 任务服务客户端
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@HttpExchange("/api/camunda/tasks")
public interface TaskServiceClient {

    @GetExchange("/{taskId}")
    @Operation(summary = "根据ID获取任务")
    Result<TaskFeignDTO> getById(@PathVariable("taskId") String taskId,
                                  @RequestParam(value = "withVariables", required = false) Boolean withVariables);

    @GetExchange
    @Operation(summary = "获取任务列表")
    Result<List<TaskFeignDTO>> getList(
            @RequestParam(value = "assignee", required = false) String assignee,
            @RequestParam(value = "candidateUser", required = false) String candidateUser,
            @RequestParam(value = "candidateGroup", required = false) String candidateGroup,
            @RequestParam(value = "processInstanceId", required = false) String processInstanceId,
            @RequestParam(value = "processDefinitionKey", required = false) String processDefinitionKey,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "tenantId", required = false) String tenantId,
            @RequestParam(value = "limit", required = false) Integer limit);

    @PostExchange("/{taskId}/claim")
    @Operation(summary = "认领任务")
    Result<Void> claim(@PathVariable("taskId") String taskId, @RequestParam("userId") String userId);

    @PostExchange("/{taskId}/unclaim")
    @Operation(summary = "取消认领任务")
    Result<Void> unclaim(@PathVariable("taskId") String taskId, @RequestParam("userId") String userId);

    @PostExchange("/{taskId}/complete")
    @Operation(summary = "完成任务")
    Result<Void> complete(@PathVariable("taskId") String taskId, @RequestBody TaskActionRequest request);

    @PostExchange("/{taskId}/delegate")
    @Operation(summary = "委派任务")
    Result<Void> delegate(@PathVariable("taskId") String taskId,
                           @RequestParam("userId") String userId,
                           @RequestParam("delegateUser") String delegateUser);

    @GetExchange("/{taskId}/variables/{variableName}")
    @Operation(summary = "获取任务变量")
    Result<Object> getVariable(@PathVariable("taskId") String taskId,
                                @PathVariable("variableName") String variableName);

    @GetExchange("/{taskId}/variables")
    @Operation(summary = "获取所有任务变量")
    Result<Map<String, Object>> getVariables(@PathVariable("taskId") String taskId);

    @PutExchange("/{taskId}/variables/{variableName}")
    @Operation(summary = "设置任务变量")
    Result<Void> setVariable(@PathVariable("taskId") String taskId,
                              @PathVariable("variableName") String variableName,
                              @RequestBody Object value);

    @DeleteExchange("/{taskId}/variables/{variableName}")
    @Operation(summary = "删除任务变量")
    Result<Void> removeVariable(@PathVariable("taskId") String taskId,
                                 @PathVariable("variableName") String variableName);

    @GetExchange("/{taskId}/check-existence")
    @Operation(summary = "检查任务是否存在")
    Result<Boolean> checkExistence(@PathVariable("taskId") String taskId);

    @GetExchange("/pending-count")
    @Operation(summary = "获取待处理任务数")
    Result<Long> getPendingTaskCount(@RequestParam("userId") String userId,
                                      @RequestParam(value = "tenantId", required = false) String tenantId);

    @GetExchange("/statistics")
    @Operation(summary = "获取任务统计")
    Result<Map<String, Object>> getStatistics(@RequestParam("userId") String userId,
                                               @RequestParam(value = "tenantId", required = false) String tenantId);

    @GetExchange("/active-by-process-instance")
    @Operation(summary = "获取流程实例的活跃任务")
    Result<List<TaskFeignDTO>> getActiveTasksByProcessInstance(
            @RequestParam("processInstanceId") String processInstanceId);
}
