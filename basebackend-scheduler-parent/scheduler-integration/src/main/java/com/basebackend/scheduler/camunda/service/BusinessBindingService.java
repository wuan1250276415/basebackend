package com.basebackend.scheduler.camunda.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.basebackend.scheduler.camunda.entity.BusinessBindingEntity;

/**
 * 业务绑定服务接口
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
public interface BusinessBindingService extends IService<BusinessBindingEntity> {

    /**
     * 根据流程 Key 获取绑定配置
     *
     * @param processKey 流程 Key
     * @param tenantId   租户 ID
     * @return 业务绑定配置
     */
    BusinessBindingEntity getByProcessKey(String processKey, String tenantId);
}
