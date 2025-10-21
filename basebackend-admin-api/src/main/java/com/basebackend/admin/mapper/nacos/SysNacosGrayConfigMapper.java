package com.basebackend.admin.mapper.nacos;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.admin.entity.nacos.SysNacosGrayConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * Nacos灰度发布配置Mapper
 */
@Mapper
public interface SysNacosGrayConfigMapper extends BaseMapper<SysNacosGrayConfig> {
}
