package com.basebackend.feign.client.scheduler;

import com.basebackend.common.model.Result;
import com.basebackend.feign.dto.scheduler.ProcessDefinitionFeignDTO;
import com.basebackend.feign.dto.scheduler.ProcessDefinitionStartRequest;
import com.basebackend.feign.fallback.scheduler.ProcessDefinitionFeignFallbackFactory;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程定义 Feign 客户端
 * 提供流程定义查询和操作接口，供其他服务调用
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@FeignClient(
        name = "basebackend-scheduler",
        contextId = "processDefinitionFeignClient",
        path = "/api/camunda/process-definitions",
        fallbackFactory = ProcessDefinitionFeignFallbackFactory.class
)
public interface ProcessDefinitionFeignClient {

    /**
     * 根据ID获取流程定义详情
     *
     * @param processDefinitionId 流程定义ID
     * @return 流程定义详情
     */
    @GetMapping("/{processDefinitionId}")
    Result<ProcessDefinitionFeignDTO> getById(
            @Parameter(description = "流程定义ID") @PathVariable("processDefinitionId") String processDefinitionId
    );

    /**
     * 根据流程定义键获取最新版本
     *
     * @param key 流程定义键
     * @param tenantId 租户ID
     * @return 流程定义列表
     */
    @GetMapping("/latest-version")
    Result<ProcessDefinitionFeignDTO> getLatestVersion(
            @Parameter(description = "流程定义键") @RequestParam("key") String key,
            @Parameter(description = "租户ID") @RequestParam(value = "tenantId", required = false) String tenantId
    );

    /**
     * 获取流程定义列表
     *
     * @param key 流程定义键（可选）
     * @param name 流程名称（可选）
     * @param tenantId 租户ID（可选）
     * @param version 版本号（可选，获取指定版本）
     * @param latestVersion 是否只获取最新版本（可选，默认true）
     * @return 流程定义列表
     */
    @GetMapping
    Result<List<ProcessDefinitionFeignDTO>> getList(
            @Parameter(description = "流程定义键") @RequestParam(value = "key", required = false) String key,
            @Parameter(description = "流程名称") @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "租户ID") @RequestParam(value = "tenantId", required = false) String tenantId,
            @Parameter(description = "版本号") @RequestParam(value = "version", required = false) Integer version,
            @Parameter(description = "是否只获取最新版本") @RequestParam(value = "latestVersion", defaultValue = "true") Boolean latestVersion
    );

    /**
     * 启动流程实例
     *
     * @param request 启动请求
     * @return 启动结果
     */
    @PostMapping("/start")
    Result<String> startProcessInstance(
            @Parameter(description = "启动流程请求") @RequestBody ProcessDefinitionStartRequest request
    );

    /**
     * 根据流程定义键启动流程实例
     *
     * @param key 流程定义键
     * @param businessKey 业务键
     * @param variables 流程变量
     * @param tenantId 租户ID
     * @return 流程实例ID
     */
    @PostMapping("/start-by-key")
    Result<String> startProcessInstanceByKey(
            @Parameter(description = "流程定义键") @RequestParam("key") String key,
            @Parameter(description = "业务键") @RequestParam(value = "businessKey", required = false) String businessKey,
            @Parameter(description = "流程变量") @RequestParam(value = "variables", required = false) String variables,
            @Parameter(description = "租户ID") @RequestParam(value = "tenantId", required = false) String tenantId
    );

    /**
     * 激活流程定义
     *
     * @param processDefinitionId 流程定义ID
     * @return 操作结果
     */
    @PutMapping("/{processDefinitionId}/activate")
    Result<Void> activate(
            @Parameter(description = "流程定义ID") @PathVariable("processDefinitionId") String processDefinitionId
    );

    /**
     * 挂起流程定义
     *
     * @param processDefinitionId 流程定义ID
     * @return 操作结果
     */
    @PutMapping("/{processDefinitionId}/suspend")
    Result<Void> suspend(
            @Parameter(description = "流程定义ID") @PathVariable("processDefinitionId") String processDefinitionId
    );

    /**
     * 检查流程定义是否存在
     *
     * @param key 流程定义键
     * @param tenantId 租户ID
     * @return 是否存在
     */
    @GetMapping("/check-existence")
    Result<Boolean> checkExistence(
            @Parameter(description = "流程定义键") @RequestParam("key") String key,
            @Parameter(description = "租户ID") @RequestParam(value = "tenantId", required = false) String tenantId
    );

    /**
     * 获取流程定义版本列表
     *
     * @param key 流程定义键
     * @param tenantId 租户ID
     * @return 版本列表
     */
    @GetMapping("/versions")
    Result<List<ProcessDefinitionFeignDTO>> getVersions(
            @Parameter(description = "流程定义键") @RequestParam("key") String key,
            @Parameter(description = "租户ID") @RequestParam(value = "tenantId", required = false) String tenantId
    );
}
