package com.basebackend.scheduler.camunda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.basebackend.scheduler.camunda.entity.BusinessBindingEntity;
import com.basebackend.scheduler.camunda.mapper.BusinessBindingMapper;
import com.basebackend.scheduler.camunda.service.BusinessBindingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 业务绑定服务实现
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessBindingServiceImpl extends ServiceImpl<BusinessBindingMapper, BusinessBindingEntity>
        implements BusinessBindingService {

    @Override
    public BusinessBindingEntity getByProcessKey(String processKey, String tenantId) {
        return this.getOne(new LambdaQueryWrapper<BusinessBindingEntity>()
                .eq(BusinessBindingEntity::getProcessKey, processKey)
                .eq(tenantId != null, BusinessBindingEntity::getTenantId, tenantId)
                .last("LIMIT 1"));
    }
}
