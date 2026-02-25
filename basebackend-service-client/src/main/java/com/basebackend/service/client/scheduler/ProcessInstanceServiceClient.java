package com.basebackend.service.client.scheduler;

import com.basebackend.api.model.scheduler.ProcessInstanceFeignDTO;
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
 * 流程实例服务客户端
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@HttpExchange("/api/camunda/process-instances")
public interface ProcessInstanceServiceClient {

    @GetExchange("/{instanceId}")
    @Operation(summary = "根据ID获取流程实例")
    Result<ProcessInstanceFeignDTO> getById(@PathVariable("instanceId") String instanceId,
                                             @RequestParam(value = "withVariables", required = false) Boolean withVariables);

    @GetExchange
    @Operation(summary = "获取流程实例列表")
    Result<List<ProcessInstanceFeignDTO>> getList(
            @RequestParam(value = "processDefinitionId", required = false) String processDefinitionId,
            @RequestParam(value = "processDefinitionKey", required = false) String processDefinitionKey,
            @RequestParam(value = "businessKey", required = false) String businessKey,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "tenantId", required = false) String tenantId,
            @RequestParam(value = "limit", required = false) Integer limit);

    @GetExchange("/by-business-key")
    @Operation(summary = "根据业务键获取流程实例")
    Result<List<ProcessInstanceFeignDTO>> getByBusinessKey(
            @RequestParam("businessKey") String businessKey,
            @RequestParam(value = "processDefinitionKey", required = false) String processDefinitionKey,
            @RequestParam(value = "tenantId", required = false) String tenantId);

    @PostExchange("/{instanceId}/activate")
    @Operation(summary = "激活流程实例")
    Result<Void> activate(@PathVariable("instanceId") String instanceId);

    @PostExchange("/{instanceId}/suspend")
    @Operation(summary = "挂起流程实例")
    Result<Void> suspend(@PathVariable("instanceId") String instanceId);

    @DeleteExchange("/{instanceId}")
    @Operation(summary = "删除流程实例")
    Result<Void> delete(@PathVariable("instanceId") String instanceId,
                         @RequestParam(value = "reason", required = false) String reason);

    @GetExchange("/{instanceId}/variables/{variableName}")
    @Operation(summary = "获取流程变量")
    Result<Object> getVariable(@PathVariable("instanceId") String instanceId,
                                @PathVariable("variableName") String variableName);

    @GetExchange("/{instanceId}/variables")
    @Operation(summary = "获取所有流程变量")
    Result<Map<String, Object>> getVariables(@PathVariable("instanceId") String instanceId);

    @PutExchange("/{instanceId}/variables/{variableName}")
    @Operation(summary = "设置流程变量")
    Result<Void> setVariable(@PathVariable("instanceId") String instanceId,
                              @PathVariable("variableName") String variableName,
                              @RequestBody Object value);

    @PutExchange("/{instanceId}/variables")
    @Operation(summary = "批量设置流程变量")
    Result<Void> setVariables(@PathVariable("instanceId") String instanceId,
                               @RequestBody Map<String, Object> variables);

    @DeleteExchange("/{instanceId}/variables/{variableName}")
    @Operation(summary = "删除流程变量")
    Result<Void> removeVariable(@PathVariable("instanceId") String instanceId,
                                 @PathVariable("variableName") String variableName);

    @GetExchange("/{instanceId}/check-existence")
    @Operation(summary = "检查流程实例是否存在")
    Result<Boolean> checkExistence(@PathVariable("instanceId") String instanceId);

    @GetExchange("/statistics")
    @Operation(summary = "获取统计信息")
    Result<Map<String, Object>> getStatistics(
            @RequestParam(value = "processDefinitionKey", required = false) String processDefinitionKey,
            @RequestParam(value = "tenantId", required = false) String tenantId);
}
