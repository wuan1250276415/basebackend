package com.basebackend.observability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.observability.entity.SlowTraceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 慢请求记录Mapper
 */
@Mapper
public interface SlowTraceRecordMapper extends BaseMapper<SlowTraceRecord> {

    /**
     * 查询指定时间范围的慢请求
     */
    List<SlowTraceRecord> selectByTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定服务的慢请求
     */
    List<SlowTraceRecord> selectByServiceName(@Param("serviceName") String serviceName);

    /**
     * 统计慢请求数量
     */
    Long countSlowTraces(
            @Param("serviceName") String serviceName,
            @Param("startTime") LocalDateTime startTime);
}
