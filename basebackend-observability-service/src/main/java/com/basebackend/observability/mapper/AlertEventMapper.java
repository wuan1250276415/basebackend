package com.basebackend.observability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.observability.entity.AlertEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警事件Mapper
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Mapper
public interface AlertEventMapper extends BaseMapper<AlertEvent> {
}
