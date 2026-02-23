package com.basebackend.messaging.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_dead_letter")
public class DeadLetterEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String messageId;

    private String topic;

    private String tags;

    private String messageType;

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
