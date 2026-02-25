package com.basebackend.observability.alert.mapper;

import com.basebackend.observability.alert.AlertEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警事件 MyBatis Mapper
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Mapper
public interface AlertEventMapper {

    int insert(AlertEvent event);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status);

    int acknowledge(@Param("id") Long id,
                    @Param("acknowledgedBy") Long acknowledgedBy,
                    @Param("acknowledgedAt") LocalDateTime acknowledgedAt);

    int resolve(@Param("id") Long id,
                @Param("resolvedBy") Long resolvedBy,
                @Param("resolvedAt") LocalDateTime resolvedAt);

    AlertEvent selectById(@Param("id") Long id);

    List<AlertEvent> selectByRuleId(@Param("ruleId") Long ruleId);

    List<AlertEvent> selectByStatus(@Param("status") String status);

    List<AlertEvent> selectBySeverity(@Param("severity") String severity);

    List<AlertEvent> selectRecent(@Param("since") LocalDateTime since);
}
