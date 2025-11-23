package com.basebackend.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.system.entity.SysLoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志Mapper接口
 */
@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {
}
