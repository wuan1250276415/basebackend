package com.basebackend.database.statistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * SQL统计实体
 * 记录SQL执行的统计信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_sql_statistics")
public class SqlStatistics extends BaseEntity {

    /**
     * SQL语句的MD5值（用于标识唯一的SQL模板）
     */
    private String sqlMd5;

    /**
     * SQL模板（参数化后的SQL语句）
     */
    private String sqlTemplate;

    /**
     * 执行次数
     */
    private Long executeCount;

    /**
     * 总执行时间（毫秒）
     */
    private Long totalTime;

    /**
     * 平均执行时间（毫秒）
     */
    private Long avgTime;

    /**
     * 最大执行时间（毫秒）
     */
    private Long maxTime;

    /**
     * 最小执行时间（毫秒）
     */
    private Long minTime;

    /**
     * 失败次数
     */
    private Long failCount;

    /**
     * 最后执行时间
     */
    private LocalDateTime lastExecuteTime;

    /**
     * 数据源名称
     */
    private String dataSourceName;

    /**
     * 租户ID（多租户场景）
     */
    private String tenantId;
}
