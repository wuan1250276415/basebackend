package com.basebackend.admin.mapper.nacos;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.admin.entity.nacos.SysNacosConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * Nacos配置Mapper
 */
@Mapper
public interface SysNacosConfigMapper extends BaseMapper<SysNacosConfig> {
}
