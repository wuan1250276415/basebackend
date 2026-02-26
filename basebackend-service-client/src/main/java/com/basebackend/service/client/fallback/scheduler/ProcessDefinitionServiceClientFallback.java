package com.basebackend.service.client.fallback.scheduler;

import com.basebackend.api.model.scheduler.ProcessDefinitionFeignDTO;
import com.basebackend.api.model.scheduler.ProcessDefinitionStartRequest;
import com.basebackend.common.model.Result;
import com.basebackend.service.client.scheduler.ProcessDefinitionServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 流程定义服务客户端降级实现
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Component
public class ProcessDefinitionServiceClientFallback implements ProcessDefinitionServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ProcessDefinitionServiceClientFallback.class);

    @Override
    public Result<ProcessDefinitionFeignDTO> getById(String processDefinitionId) {
        log.error("[服务降级] 获取流程定义失败: id={}", processDefinitionId);
        return Result.error("调度器服务不可用，获取流程定义失败");
    }

    @Override
    public Result<ProcessDefinitionFeignDTO> getLatestVersion(String key, String tenantId) {
        log.error("[服务降级] 获取最新版本流程定义失败: key={}", key);
        return Result.error("调度器服务不可用");
    }

    @Override
    public Result<List<ProcessDefinitionFeignDTO>> getList(String key, String name, String tenantId,
            Integer version, Boolean latestVersion) {
        log.error("[服务降级] 获取流程定义列表失败");
        return Result.success("调度器服务降级，返回空列表", Collections.emptyList());
    }

    @Override
    public Result<String> startProcessInstance(ProcessDefinitionStartRequest request) {
        log.error("[服务降级] 流程启动失败: key={}", request != null ? request.processDefinitionKey() : null);
        return Result.error("调度器服务不可用，流程启动失败");
    }

    @Override
    public Result<String> startProcessInstanceByKey(String key, String businessKey, String variables, String tenantId) {
        log.error("[服务降级] 根据Key启动流程实例失败: key={}", key);
        return Result.error("调度器服务不可用，流程启动失败");
    }

    @Override
    public Result<Void> activate(String processDefinitionId) {
        log.error("[服务降级] 激活流程定义失败: id={}", processDefinitionId);
        return Result.error("调度器服务不可用");
    }

    @Override
    public Result<Void> suspend(String processDefinitionId) {
        log.error("[服务降级] 挂起流程定义失败: id={}", processDefinitionId);
        return Result.error("调度器服务不可用");
    }

    @Override
    public Result<Boolean> checkExistence(String key, String tenantId) {
        log.error("[服务降级] 检查流程定义是否存在失败: key={}", key);
        return Result.success("调度器服务降级", false);
    }

    @Override
    public Result<List<ProcessDefinitionFeignDTO>> getVersions(String key, String tenantId) {
        log.error("[服务降级] 获取流程定义版本列表失败: key={}", key);
        return Result.success("调度器服务降级，返回空列表", Collections.emptyList());
    }
}
