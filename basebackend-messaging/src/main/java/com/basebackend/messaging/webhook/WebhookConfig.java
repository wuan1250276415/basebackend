package com.basebackend.messaging.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Webhook配置模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookConfig {

    /**
     * 配置ID
     */
    private Long id;

    /**
     * Webhook名称
     */
    private String name;

    /**
     * Webhook URL
     */
    private String url;

    /**
     * 订阅的事件类型（多个用逗号分隔）
     */
    private String eventTypes;

    /**
     * 签名密钥
     */
    private String secret;

    /**
     * 是否启用签名验证
     */
    private Boolean signatureEnabled;

    /**
     * HTTP请求方法
     */
    private String method;

    /**
     * 自定义请求头（JSON格式）
     */
    private String headers;

    /**
     * 超时时间（秒）
     */
    private Integer timeout;

    /**
     * 最大重试次数
     */
    private Integer maxRetries;

    /**
     * 重试间隔（秒）
     */
    private Integer retryInterval;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    private Long createBy;

    /**
     * 备注
     */
    private String remark;
}
