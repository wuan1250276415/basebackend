package com.basebackend.observability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 追踪Span扩展实体
 */
@Data
@TableName("trace_span_ext")
public class TraceSpanExt {

    @TableId(value = "span_id", type = IdType.INPUT)
    private String spanId;

    @TableField("trace_id")
    private String traceId;

    @TableField("parent_span_id")
    private String parentSpanId;

    @TableField("service_name")
    private String serviceName;

    @TableField("operation_name")
    private String operationName;

    @TableField("start_time")
    private Long startTime;

    @TableField("duration")
    private Long duration;

    @TableField("tags")
    private String tags;

    @TableField("logs")
    private String logs;

    @TableField("status")
    private String status;

    @TableField("error_message")
    private String errorMessage;

    @TableField("stack_trace")
    private String stackTrace;

    @TableField("create_time")
    private LocalDateTime createTime;
}
