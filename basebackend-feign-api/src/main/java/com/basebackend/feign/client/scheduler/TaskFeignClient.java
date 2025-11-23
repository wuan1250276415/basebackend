package com.basebackend.feign.client.scheduler;

import com.basebackend.common.model.Result;
import com.basebackend.feign.dto.scheduler.TaskActionRequest;
import com.basebackend.feign.dto.scheduler.TaskFeignDTO;
import com.basebackend.feign.fallback.scheduler.TaskFeignFallbackFactory;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 任务 Feign 客户端
 * 提供任务查询和操作接口，供其他服务调用
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@FeignClient(
        name = "basebackend-scheduler",
        contextId = "taskFeignClient",
        path = "/api/camunda/tasks",
        fallbackFactory = TaskFeignFallbackFactory.class
)
public interface TaskFeignClient {

    /**
     * 根据ID获取任务详情
     *
     * @param taskId 任务ID
     * @param withVariables 是否包含任务变量
     * @return 任务详情
     */
    @GetMapping("/{taskId}")
    Result<TaskFeignDTO> getById(
            @Parameter(description = "任务ID") @PathVariable("taskId") String taskId,
            @Parameter(description = "是否包含任务变量") @RequestParam(value = "withVariables", defaultValue = "false") Boolean withVariables
    );

    /**
     * 获取任务列表
     *
     * @param assignee 任务分配人（可选）
     * @param candidateUser 候选用户（可选）
     * @param candidateGroup 候选组（可选）
     * @param processInstanceId 流程实例ID（可选）
     * @param processDefinitionKey 流程定义键（可选）
     * @param name 任务名称（可选）
     * @param state 状态（可选：open、completed、cancelled）
     * @param tenantId 租户ID（可选）
     * @param limit 返回数量限制（可选，默认100）
     * @return 任务列表
     */
    @GetMapping
    Result<List<TaskFeignDTO>> getList(
            @Parameter(description = "任务分配人") @RequestParam(value = "assignee", required = false) String assignee,
            @Parameter(description = "候选用户") @RequestParam(value = "candidateUser", required = false) String candidateUser,
            @Parameter(description = "候选组") @RequestParam(value = "candidateGroup", required = false) String candidateGroup,
            @Parameter(description = "流程实例ID") @RequestParam(value = "processInstanceId", required = false) String processInstanceId,
            @Parameter(description = "流程定义键") @RequestParam(value = "processDefinitionKey", required = false) String processDefinitionKey,
            @Parameter(description = "任务名称") @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "状态") @RequestParam(value = "state", required = false) String state,
            @Parameter(description = "租户ID") @RequestParam(value = "tenantId", required = false) String tenantId,
            @Parameter(description = "返回数量限制") @RequestParam(value = "limit", defaultValue = "100") Integer limit
    );

    /**
     * 认领任务
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/{taskId}/claim")
    Result<Void> claim(
            @Parameter(description = "任务ID") @PathVariable("taskId") String taskId,
            @Parameter(description = "用户ID") @RequestParam("userId") String userId
    );

    /**
     * 释放任务
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/{taskId}/unclaim")
    Result<Void> unclaim(
            @Parameter(description = "任务ID") @PathVariable("taskId") String taskId,
            @Parameter(description = "用户ID") @RequestParam("userId") String userId
    );

    /**
     * 完成任务
     *
     * @param taskId 任务ID
     * @param request 任务操作请求
     * @return 操作结果
     */
    @PostMapping("/{taskId}/complete")
    Result<Void> complete(
            @Parameter(description = "任务ID") @PathVariable("taskId") String taskId,
            @Parameter(description = "任务操作请求") @RequestBody(required = false) TaskActionRequest request
    );

    /**
     * 委派任务
     *
     * @param taskId 任务ID
     * @param userId 委派目标用户ID
     * @param delegateUser 委派发起人ID
     * @return 操作结果
     */
    @PostMapping("/{taskId}/delegate")
    Result<Void> delegate(
            @Parameter(description = "任务ID") @PathVariable("taskId") String taskId,
            @Parameter(description = "委派目标用户ID") @RequestParam("userId") String userId,
            @Parameter(description = "委派发起人ID") @RequestParam("delegateUser") String delegateUser
    );

    /**
     * 获取任务变量
     *
     * @param taskId 任务ID
     * @param variableName 变量名
     * @return 变量值
     */
    @GetMapping("/{taskId}/variables/{variableName}")
    Result<Object> getVariable(
            @Parameter(description = "任务ID") @PathVariable("taskId") String taskId,
            @Parameter(description = "变量名") @PathVariable("variableName") String variableName
    );

    /**
     * 获取任务所有变量
     *
     * @param taskId 任务ID
     * @return 任务变量Map
     */
    @GetMapping("/{taskId}/variables")
    Result<Map<String, Object>> getVariables(
            @Parameter(description = "任务ID") @PathVariable("taskId") String taskId
    );

    /**
     * 设置任务变量
     *
     * @param taskId 任务ID
     * @param variableName 变量名
     * @param value 变量值
     * @return 操作结果
     */
    @PutMapping("/{taskId}/variables/{variableName}")
    Result<Void> setVariable(
            @Parameter(description = "任务ID") @PathVariable("taskId") String taskId,
            @Parameter(description = "变量名") @PathVariable("variableName") String variableName,
            @Parameter(description = "变量值") @RequestBody Object value
    );

    /**
     * 删除任务变量
     *
     * @param taskId 任务ID
     * @param variableName 变量名
     * @return 操作结果
     */
    @DeleteMapping("/{taskId}/variables/{variableName}")
    Result<Void> removeVariable(
            @Parameter(description = "任务ID") @PathVariable("taskId") String taskId,
            @Parameter(description = "变量名") @PathVariable("variableName") String variableName
    );

    /**
     * 检查任务是否存在
     *
     * @param taskId 任务ID
     * @return 是否存在
     */
    @GetMapping("/check-existence")
    Result<Boolean> checkExistence(
            @Parameter(description = "任务ID") @RequestParam("taskId") String taskId
    );

    /**
     * 获取用户待办任务数量
     *
     * @param userId 用户ID
     * @param tenantId 租户ID（可选）
     * @return 待办任务数量
     */
    @GetMapping("/count/pending")
    Result<Long> getPendingTaskCount(
            @Parameter(description = "用户ID") @RequestParam("userId") String userId,
            @Parameter(description = "租户ID") @RequestParam(value = "tenantId", required = false) String tenantId
    );

    /**
     * 获取任务统计信息
     *
     * @param userId 用户ID（可选）
     * @param tenantId 租户ID（可选）
     * @return 统计信息
     */
    @GetMapping("/statistics")
    Result<Map<String, Object>> getStatistics(
            @Parameter(description = "用户ID") @RequestParam(value = "userId", required = false) String userId,
            @Parameter(description = "租户ID") @RequestParam(value = "tenantId", required = false) String tenantId
    );

    /**
     * 根据流程实例ID获取当前活动任务
     *
     * @param processInstanceId 流程实例ID
     * @return 当前活动任务列表
     */
    @GetMapping("/active/by-process-instance")
    Result<List<TaskFeignDTO>> getActiveTasksByProcessInstance(
            @Parameter(description = "流程实例ID") @RequestParam("processInstanceId") String processInstanceId
    );
}
