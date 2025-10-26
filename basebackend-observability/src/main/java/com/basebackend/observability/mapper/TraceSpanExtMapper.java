package com.basebackend.observability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.observability.entity.TraceSpanExt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 追踪Span扩展Mapper
 */
@Mapper
public interface TraceSpanExtMapper extends BaseMapper<TraceSpanExt> {

    /**
     * 根据traceId查询所有Span
     */
    List<TraceSpanExt> selectByTraceId(@Param("traceId") String traceId);

    /**
     * 查询指定时间范围内的Span
     */
    List<TraceSpanExt> selectByTimeRange(
            @Param("startTime") Long startTime, 
            @Param("endTime") Long endTime);

    /**
     * 查询指定服务的Span
     */
    List<TraceSpanExt> selectByServiceName(@Param("serviceName") String serviceName);

    /**
     * 查询错误的Span
     */
    List<TraceSpanExt> selectErrorSpans(
            @Param("startTime") Long startTime, 
            @Param("endTime") Long endTime);
}
