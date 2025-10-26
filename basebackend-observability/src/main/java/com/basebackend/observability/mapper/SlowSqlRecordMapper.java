package com.basebackend.observability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.observability.entity.SlowSqlRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 慢SQL记录Mapper
 */
@Mapper
public interface SlowSqlRecordMapper extends BaseMapper<SlowSqlRecord> {

    /**
     * 查询指定时间范围的慢SQL
     */
    List<SlowSqlRecord> selectByTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 按方法聚合统计
     */
    List<Map<String, Object>> aggregateByMethod(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 获取Top N慢SQL
     */
    List<SlowSqlRecord> selectTopSlowSql(@Param("limit") int limit);
}
