package com.basebackend.observability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.observability.entity.AlertRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警规则Mapper
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Mapper
public interface AlertRuleMapper extends BaseMapper<AlertRule> {
}
