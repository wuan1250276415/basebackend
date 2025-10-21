package com.basebackend.admin.mapper.nacos;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.admin.entity.nacos.SysNacosConfigHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * Nacos配置历史Mapper
 */
@Mapper
public interface SysNacosConfigHistoryMapper extends BaseMapper<SysNacosConfigHistory> {
}
