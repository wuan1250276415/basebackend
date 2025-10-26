package com.basebackend.observability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.observability.entity.ExceptionAggregation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 异常聚合Mapper
 */
@Mapper
public interface ExceptionAggregationMapper extends BaseMapper<ExceptionAggregation> {

    /**
     * 查询Top N异常
     */
    List<ExceptionAggregation> selectTopExceptions(
            @Param("limit") int limit,
            @Param("startTime") LocalDateTime startTime);

    /**
     * 查询指定服务的异常
     */
    List<ExceptionAggregation> selectByService(@Param("serviceName") String serviceName);

    /**
     * 更新异常次数
     */
    int incrementOccurrence(@Param("stackTraceHash") String stackTraceHash);
}
