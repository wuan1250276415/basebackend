package com.basebackend.database.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.database.tenant.entity.TenantConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户配置 Mapper
 */
@Mapper
public interface TenantConfigMapper extends BaseMapper<TenantConfig> {
}
