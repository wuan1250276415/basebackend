package com.basebackend.observability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.observability.entity.JvmMetrics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JVM性能指标Mapper
 */
@Mapper
public interface JvmMetricsMapper extends BaseMapper<JvmMetrics> {

    /**
     * 查询指定实例的指标
     */
    List<JvmMetrics> selectByInstance(@Param("instanceId") String instanceId);

    /**
     * 查询指定时间范围的指标
     */
    List<JvmMetrics> selectByTimeRange(
            @Param("instanceId") String instanceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 获取最新的指标
     */
    JvmMetrics selectLatest(@Param("instanceId") String instanceId);

    /**
     * 删除过期数据
     */
    int deleteExpired(@Param("beforeTime") LocalDateTime beforeTime);
}
