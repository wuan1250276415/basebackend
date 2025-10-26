package com.basebackend.observability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 慢SQL记录实体
 */
@Data
@TableName("slow_sql_record")
public class SlowSqlRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("method_name")
    private String methodName;

    @TableField("sql_statement")
    private String sqlStatement;

    @TableField("duration")
    private Long duration;

    @TableField("parameters")
    private String parameters;

    @TableField("trace_id")
    private String traceId;

    @TableField("service_name")
    private String serviceName;

    @TableField("timestamp")
    private LocalDateTime timestamp;
}
