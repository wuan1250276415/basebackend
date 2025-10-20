package com.basebackend.admin.entity.messaging;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Webhook调用日志实体
 */
@Data
@TableName("sys_webhook_log")
public class SysWebhookLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long webhookId;

    private String eventId;

    private String eventType;

    private String requestUrl;

    private String requestMethod;

    private String requestHeaders;

    private String requestBody;

    private Integer responseStatus;

    private String responseBody;

    private Long responseTime;

    private Boolean success;

    private String errorMessage;

    private Integer retryCount;

    private LocalDateTime callTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
