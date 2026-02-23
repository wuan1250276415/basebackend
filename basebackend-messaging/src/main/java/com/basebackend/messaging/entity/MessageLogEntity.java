package com.basebackend.messaging.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_message_log")
public class MessageLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String messageId;

    private String mqMessageId;

    private String topic;

    private String tag;

    private String payload;

    private String headers;

    private LocalDateTime sendTime;

    private Long delayMillis;

    private Integer retryCount;

    private Integer maxRetries;

    private String partitionKey;

    private String status;

    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
