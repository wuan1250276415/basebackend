package com.basebackend.admin.mapper.nacos;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.admin.entity.nacos.SysNacosPublishTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * Nacos配置发布任务Mapper
 */
@Mapper
public interface SysNacosPublishTaskMapper extends BaseMapper<SysNacosPublishTask> {
}
