package com.basebackend.admin.entity.messaging;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 死信实体
 */
@Data
@TableName("sys_dead_letter")
public class SysDeadLetter {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String messageId;

    private String topic;

    private String routingKey;

    private String payload;

    private String headers;

    private Integer retryCount;

    private String errorMessage;

    private String originalQueue;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private LocalDateTime handledTime;

    private Long handledBy;
}
