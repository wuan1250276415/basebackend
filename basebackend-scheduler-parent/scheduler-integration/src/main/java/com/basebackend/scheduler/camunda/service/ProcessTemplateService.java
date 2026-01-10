package com.basebackend.scheduler.camunda.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.basebackend.scheduler.camunda.entity.ProcessTemplateEntity;

/**
 * 工作流模板服务接口
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
public interface ProcessTemplateService extends IService<ProcessTemplateEntity> {

    /**
     * 根据 Key 获取最新版本的表单模板
     *
     * @param templateKey 模板 Key
     * @param tenantId    租户 ID
     * @return 模板实体
     */
    ProcessTemplateEntity getByKey(String templateKey, String tenantId);

    /**
     * 绑定 Camunda 部署 ID
     *
     * @param templateId   模板 ID
     * @param deploymentId 部署 ID
     */
    void bindDeployment(Long templateId, String deploymentId);
}
