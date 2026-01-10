package com.basebackend.scheduler.camunda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.basebackend.scheduler.camunda.entity.ProcessTemplateEntity;
import com.basebackend.scheduler.camunda.mapper.ProcessTemplateMapper;
import com.basebackend.scheduler.camunda.service.ProcessTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 工作流模板服务实现
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessTemplateServiceImpl extends ServiceImpl<ProcessTemplateMapper, ProcessTemplateEntity>
        implements ProcessTemplateService {

    @Override
    public ProcessTemplateEntity getByKey(String templateKey, String tenantId) {
        return this.getOne(new LambdaQueryWrapper<ProcessTemplateEntity>()
                .eq(ProcessTemplateEntity::getTemplateKey, templateKey)
                .eq(tenantId != null, ProcessTemplateEntity::getTenantId, tenantId)
        // 如果有多个，取最新的（这里假设 Key+Tenant 是 Unique 的，或者业务逻辑保证）
        // 实际表结构 Key+Tenant 是 Unique Key
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindDeployment(Long templateId, String deploymentId) {
        ProcessTemplateEntity entity = new ProcessTemplateEntity();
        entity.setId(templateId);
        entity.setDeploymentId(deploymentId);
        entity.setStatus(1); // 发布状态
        this.updateById(entity);
    }
}
