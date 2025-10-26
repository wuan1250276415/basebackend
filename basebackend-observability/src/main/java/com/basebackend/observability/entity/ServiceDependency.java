package com.basebackend.observability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务依赖关系实体
 */
@Data
@TableName("service_dependency")
public class ServiceDependency {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("from_service")
    private String fromService;

    @TableField("to_service")
    private String toService;

    @TableField("call_count")
    private Long callCount;

    @TableField("error_count")
    private Long errorCount;

    @TableField("total_duration")
    private Long totalDuration;

    @TableField("time_bucket")
    private LocalDateTime timeBucket;
}
