package com.basebackend.observability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * JVM性能指标实体
 */
@Data
@Builder
@TableName("jvm_metrics")
public class JvmMetrics {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("instance_id")
    private String instanceId;

    @TableField("timestamp")
    private LocalDateTime timestamp;

    @TableField("heap_used")
    private Long heapUsed;

    @TableField("heap_max")
    private Long heapMax;

    @TableField("heap_committed")
    private Long heapCommitted;

    @TableField("non_heap_used")
    private Long nonHeapUsed;

    @TableField("thread_count")
    private Integer threadCount;

    @TableField("daemon_thread_count")
    private Integer daemonThreadCount;

    @TableField("peak_thread_count")
    private Integer peakThreadCount;

    @TableField("gc_count")
    private Integer gcCount;

    @TableField("gc_time")
    private Long gcTime;

    @TableField("cpu_usage")
    private Double cpuUsage;

    @TableField("load_average")
    private Double loadAverage;
}
