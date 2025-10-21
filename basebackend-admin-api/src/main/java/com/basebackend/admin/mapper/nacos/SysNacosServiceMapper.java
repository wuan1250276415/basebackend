package com.basebackend.admin.mapper.nacos;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.admin.entity.nacos.SysNacosService;
import org.apache.ibatis.annotations.Mapper;

/**
 * Nacos服务注册Mapper
 */
@Mapper
public interface SysNacosServiceMapper extends BaseMapper<SysNacosService> {
}
