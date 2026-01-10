package com.basebackend.scheduler.camunda.service;

import com.basebackend.scheduler.camunda.dto.ProcessInstanceMigrationRequest;

/**
 * 流程实例迁移服务
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
public interface ProcessMigrationService {

    /**
     * 执行流程实例迁移
     *
     * @param request 迁移请求
     */
    void migrateProcessInstances(ProcessInstanceMigrationRequest request);
}
