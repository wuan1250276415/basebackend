package com.basebackend.database.statistics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SQL执行信息
 * 用于在拦截器和收集器之间传递SQL执行数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlExecutionInfo {

    /**
     * SQL语句
     */
    private String sql;

    /**
     * SQL模板（参数化后的SQL）
     */
    private String sqlTemplate;

    /**
     * SQL的MD5值
     */
    private String sqlMd5;

    /**
     * 执行时间（毫秒）
     */
    private Long executionTime;

    /**
     * 影响行数
     */
    private Integer affectedRows;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 失败原因
     */
    private String failureReason;

    /**
     * 执行时间
     */
    private LocalDateTime executeTime;

    /**
     * 数据源名称
     */
    private String dataSourceName;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * Mapper方法ID
     */
    private String mapperId;
}
