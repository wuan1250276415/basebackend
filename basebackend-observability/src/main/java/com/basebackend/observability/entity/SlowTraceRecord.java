package com.basebackend.observability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 慢请求记录实体
 */
@Data
@TableName("slow_trace_record")
public class SlowTraceRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("trace_id")
    private String traceId;

    @TableField("service_name")
    private String serviceName;

    @TableField("operation_name")
    private String operationName;

    @TableField("duration")
    private Long duration;

    @TableField("threshold")
    private Long threshold;

    @TableField("bottleneck_type")
    private String bottleneckType;

    @TableField("bottleneck_spans")
    private String bottleneckSpans;

    @TableField("create_time")
    private LocalDateTime createTime;
}
