package com.basebackend.scheduler.camunda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.scheduler.camunda.entity.NodeConfigEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 节点配置 Mapper
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Mapper
public interface NodeConfigMapper extends BaseMapper<NodeConfigEntity> {
}
