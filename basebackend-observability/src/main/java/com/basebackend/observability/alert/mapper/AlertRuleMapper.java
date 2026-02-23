package com.basebackend.observability.alert.mapper;

import com.basebackend.observability.alert.AlertRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 告警规则 MyBatis Mapper
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Mapper
public interface AlertRuleMapper {

    int insert(AlertRule rule);

    int update(AlertRule rule);

    AlertRule selectById(@Param("id") Long id);

    List<AlertRule> selectAll();

    List<AlertRule> selectByEnabled(@Param("enabled") Boolean enabled);

    List<AlertRule> selectBySeverity(@Param("severity") String severity);

    AlertRule selectByRuleName(@Param("ruleName") String ruleName);

    int deleteById(@Param("id") Long id);

    long count();
}
