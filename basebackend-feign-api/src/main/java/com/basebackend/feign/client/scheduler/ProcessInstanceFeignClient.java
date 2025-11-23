package com.basebackend.feign.client.scheduler;

import com.basebackend.common.model.Result;
import com.basebackend.feign.dto.scheduler.ProcessInstanceFeignDTO;
import com.basebackend.feign.fallback.scheduler.ProcessInstanceFeignFallbackFactory;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 流程实例 Feign 客户端
 * 提供流程实例查询和操作接口，供其他服务调用
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@FeignClient(
        name = "basebackend-scheduler",
        contextId = "processInstanceFeignClient",
        path = "/api/camunda/process-instances",
        fallbackFactory = ProcessInstanceFeignFallbackFactory.class
)
public interface ProcessInstanceFeignClient {

    /**
     * 根据ID获取流程实例详情
     *
     * @param instanceId 流程实例ID
     * @param withVariables 是否包含流程变量
     * @return 流程实例详情
     */
    @GetMapping("/{instanceId}")
    Result<ProcessInstanceFeignDTO> getById(
            @Parameter(description = "流程实例ID") @PathVariable("instanceId") String instanceId,
            @Parameter(description = "是否包含流程变量") @RequestParam(value = "withVariables", defaultValue = "false") Boolean withVariables
    );

    /**
     * 获取流程实例列表
     *
     * @param processDefinitionId 流程定义ID（可选）
     * @param processDefinitionKey 流程定义键（可选）
     * @param businessKey 业务键（可选）
     * @param state 状态（可选：running、completed、suspended）
     * @param tenantId 租户ID（可选）
     * @param limit 返回数量限制（可选，默认100）
     * @return 流程实例列表
     */
    @GetMapping
    Result<List<ProcessInstanceFeignDTO>> getList(
            @Parameter(description = "流程定义ID") @RequestParam(value = "processDefinitionId", required = false) String processDefinitionId,
            @Parameter(description = "流程定义键") @RequestParam(value = "processDefinitionKey", required = false) String processDefinitionKey,
            @Parameter(description = "业务键") @RequestParam(value = "businessKey", required = false) String businessKey,
            @Parameter(description = "状态") @RequestParam(value = "state", required = false) String state,
            @Parameter(description = "租户ID") @RequestParam(value = "tenantId", required = false) String tenantId,
            @Parameter(description = "返回数量限制") @RequestParam(value = "limit", defaultValue = "100") Integer limit
    );

    /**
     * 根据业务键获取流程实例
     *
     * @param businessKey 业务键
     * @param processDefinitionKey 流程定义键（可选）
     * @param tenantId 租户ID（可选）
     * @return 流程实例列表
     */
    @GetMapping("/by-business-key")
    Result<List<ProcessInstanceFeignDTO>> getByBusinessKey(
            @Parameter(description = "业务键") @RequestParam("businessKey") String businessKey,
            @Parameter(description = "流程定义键") @RequestParam(value = "processDefinitionKey", required = false) String processDefinitionKey,
            @Parameter(description = "租户ID") @RequestParam(value = "tenantId", required = false) String tenantId
    );

    /**
     * 激活流程实例
     *
     * @param instanceId 流程实例ID
     * @return 操作结果
     */
    @PutMapping("/{instanceId}/activate")
    Result<Void> activate(
            @Parameter(description = "流程实例ID") @PathVariable("instanceId") String instanceId
    );

    /**
     * 挂起流程实例
     *
     * @param instanceId 流程实例ID
     * @return 操作结果
     */
    @PutMapping("/{instanceId}/suspend")
    Result<Void> suspend(
            @Parameter(description = "流程实例ID") @PathVariable("instanceId") String instanceId
    );

    /**
     * 删除流程实例
     *
     * @param instanceId 流程实例ID
     * @param reason 删除原因（可选）
     * @return 操作结果
     */
    @DeleteMapping("/{instanceId}")
    Result<Void> delete(
            @Parameter(description = "流程实例ID") @PathVariable("instanceId") String instanceId,
            @Parameter(description = "删除原因") @RequestParam(value = "reason", required = false) String reason
    );

    /**
     * 获取流程变量
     *
     * @param instanceId 流程实例ID
     * @param variableName 变量名
     * @return 变量值
     */
    @GetMapping("/{instanceId}/variables/{variableName}")
    Result<Object> getVariable(
            @Parameter(description = "流程实例ID") @PathVariable("instanceId") String instanceId,
            @Parameter(description = "变量名") @PathVariable("variableName") String variableName
    );

    /**
     * 获取所有流程变量
     *
     * @param instanceId 流程实例ID
     * @return 流程变量Map
     */
    @GetMapping("/{instanceId}/variables")
    Result<Map<String, Object>> getVariables(
            @Parameter(description = "流程实例ID") @PathVariable("instanceId") String instanceId
    );

    /**
     * 设置流程变量
     *
     * @param instanceId 流程实例ID
     * @param variableName 变量名
     * @param value 变量值
     * @return 操作结果
     */
    @PutMapping("/{instanceId}/variables/{variableName}")
    Result<Void> setVariable(
            @Parameter(description = "流程实例ID") @PathVariable("instanceId") String instanceId,
            @Parameter(description = "变量名") @PathVariable("variableName") String variableName,
            @Parameter(description = "变量值") @RequestBody Object value
    );

    /**
     * 批量设置流程变量
     *
     * @param instanceId 流程实例ID
     * @param variables 变量Map
     * @return 操作结果
     */
    @PutMapping("/{instanceId}/variables")
    Result<Void> setVariables(
            @Parameter(description = "流程实例ID") @PathVariable("instanceId") String instanceId,
            @Parameter(description = "变量Map") @RequestBody Map<String, Object> variables
    );

    /**
     * 删除流程变量
     *
     * @param instanceId 流程实例ID
     * @param variableName 变量名
     * @return 操作结果
     */
    @DeleteMapping("/{instanceId}/variables/{variableName}")
    Result<Void> removeVariable(
            @Parameter(description = "流程实例ID") @PathVariable("instanceId") String instanceId,
            @Parameter(description = "变量名") @PathVariable("variableName") String variableName
    );

    /**
     * 检查流程实例是否存在
     *
     * @param instanceId 流程实例ID
     * @return 是否存在
     */
    @GetMapping("/check-existence")
    Result<Boolean> checkExistence(
            @Parameter(description = "流程实例ID") @RequestParam("instanceId") String instanceId
    );

    /**
     * 获取流程实例统计信息
     *
     * @param processDefinitionKey 流程定义键（可选）
     * @param tenantId 租户ID（可选）
     * @return 统计信息
     */
    @GetMapping("/statistics")
    Result<Map<String, Object>> getStatistics(
            @Parameter(description = "流程定义键") @RequestParam(value = "processDefinitionKey", required = false) String processDefinitionKey,
            @Parameter(description = "租户ID") @RequestParam(value = "tenantId", required = false) String tenantId
    );
}
