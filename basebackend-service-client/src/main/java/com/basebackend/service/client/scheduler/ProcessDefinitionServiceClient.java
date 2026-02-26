package com.basebackend.service.client.scheduler;

import com.basebackend.api.model.scheduler.ProcessDefinitionFeignDTO;
import com.basebackend.api.model.scheduler.ProcessDefinitionStartRequest;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * 流程定义服务客户端
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@HttpExchange("/api/camunda/process-definitions")
public interface ProcessDefinitionServiceClient {

    @GetExchange("/{processDefinitionId}")
    @Operation(summary = "根据ID获取流程定义")
    Result<ProcessDefinitionFeignDTO> getById(@PathVariable("processDefinitionId") String processDefinitionId);

    @GetExchange("/latest")
    @Operation(summary = "获取最新版本流程定义")
    Result<ProcessDefinitionFeignDTO> getLatestVersion(@RequestParam("key") String key,
                                                        @RequestParam(value = "tenantId", required = false) String tenantId);

    @GetExchange
    @Operation(summary = "获取流程定义列表")
    Result<List<ProcessDefinitionFeignDTO>> getList(
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "tenantId", required = false) String tenantId,
            @RequestParam(value = "version", required = false) Integer version,
            @RequestParam(value = "latestVersion", required = false) Boolean latestVersion);

    @PostExchange("/start")
    @Operation(summary = "启动流程实例")
    Result<String> startProcessInstance(@RequestBody ProcessDefinitionStartRequest request);

    @PostExchange("/start-by-key")
    @Operation(summary = "根据Key启动流程实例")
    Result<String> startProcessInstanceByKey(
            @RequestParam("key") String key,
            @RequestParam(value = "businessKey", required = false) String businessKey,
            @RequestParam(value = "variables", required = false) String variables,
            @RequestParam(value = "tenantId", required = false) String tenantId);

    @PostExchange("/{processDefinitionId}/activate")
    @Operation(summary = "激活流程定义")
    Result<Void> activate(@PathVariable("processDefinitionId") String processDefinitionId);

    @PostExchange("/{processDefinitionId}/suspend")
    @Operation(summary = "挂起流程定义")
    Result<Void> suspend(@PathVariable("processDefinitionId") String processDefinitionId);

    @GetExchange("/check-existence")
    @Operation(summary = "检查流程定义是否存在")
    Result<Boolean> checkExistence(@RequestParam("key") String key,
                                    @RequestParam(value = "tenantId", required = false) String tenantId);

    @GetExchange("/versions")
    @Operation(summary = "获取流程定义版本列表")
    Result<List<ProcessDefinitionFeignDTO>> getVersions(@RequestParam("key") String key,
                                                         @RequestParam(value = "tenantId", required = false) String tenantId);
}
