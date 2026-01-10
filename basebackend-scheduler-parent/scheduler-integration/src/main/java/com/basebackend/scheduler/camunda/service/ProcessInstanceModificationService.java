package com.basebackend.scheduler.camunda.service;

import com.basebackend.scheduler.camunda.dto.ProcessInstanceModificationRequest;

/**
 * 流程实例修改服务（跳转/回退）
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
public interface ProcessInstanceModificationService {

    /**
     * 修改流程实例（跳转/回退）
     *
     * @param instanceId 流程实例ID
     * @param request    修改请求
     */
    void modifyProcessInstance(String instanceId, ProcessInstanceModificationRequest request);
}
