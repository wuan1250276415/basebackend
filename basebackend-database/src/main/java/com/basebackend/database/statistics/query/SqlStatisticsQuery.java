package com.basebackend.database.statistics.query;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * SQL统计查询条件
 */
@Data
public class SqlStatisticsQuery {

    /**
     * SQL模板（模糊查询）
     */
    private String sqlTemplate;

    /**
     * 数据源名称
     */
    private String dataSourceName;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 最小执行次数
     */
    private Long minExecuteCount;

    /**
     * 最小平均执行时间（毫秒）
     */
    private Long minAvgTime;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 排序字段
     * 可选值: executeCount, avgTime, maxTime, totalTime, failCount
     */
    private String orderBy = "executeCount";

    /**
     * 排序方向
     * 可选值: ASC, DESC
     */
    private String orderDirection = "DESC";

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 20;
}
