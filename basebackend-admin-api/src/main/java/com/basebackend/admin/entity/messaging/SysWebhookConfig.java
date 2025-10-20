package com.basebackend.admin.entity.messaging;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Webhook配置实体
 */
@Data
@TableName("sys_webhook_config")
public class SysWebhookConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String url;

    private String eventTypes;

    private String secret;

    private Boolean signatureEnabled;

    private String method;

    private String headers;

    private Integer timeout;

    private Integer maxRetries;

    private Integer retryInterval;

    private Boolean enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    private String remark;

    @TableLogic
    private Boolean deleted;
}
