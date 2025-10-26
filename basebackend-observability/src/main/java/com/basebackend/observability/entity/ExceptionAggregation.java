package com.basebackend.observability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 异常聚合实体
 */
@Data
@TableName("exception_aggregation")
public class ExceptionAggregation {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("exception_class")
    private String exceptionClass;

    @TableField("exception_message")
    private String exceptionMessage;

    @TableField("stack_trace_hash")
    private String stackTraceHash;

    @TableField("occurrence_count")
    private Long occurrenceCount;

    @TableField("first_seen")
    private LocalDateTime firstSeen;

    @TableField("last_seen")
    private LocalDateTime lastSeen;

    @TableField("sample_log_id")
    private String sampleLogId;

    @TableField("service_name")
    private String serviceName;

    @TableField("status")
    private String status;

    @TableField("severity")
    private String severity;
}
