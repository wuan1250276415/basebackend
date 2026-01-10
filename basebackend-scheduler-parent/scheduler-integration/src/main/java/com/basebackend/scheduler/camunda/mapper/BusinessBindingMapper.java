package com.basebackend.scheduler.camunda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.scheduler.camunda.entity.BusinessBindingEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务绑定 Mapper
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Mapper
public interface BusinessBindingMapper extends BaseMapper<BusinessBindingEntity> {
}
