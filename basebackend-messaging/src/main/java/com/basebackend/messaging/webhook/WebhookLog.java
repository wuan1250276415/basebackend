package com.basebackend.messaging.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Webhook调用日志模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookLog {

    /**
     * 日志ID
     */
    private Long id;

    /**
     * Webhook配置ID
     */
    private Long webhookId;

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求头
     */
    private String requestHeaders;

    /**
     * 请求体
     */
    private String requestBody;

    /**
     * 响应状态码
     */
    private Integer responseStatus;

    /**
     * 响应体
     */
    private String responseBody;

    /**
     * 响应时间（毫秒）
     */
    private Long responseTime;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 调用时间
     */
    private LocalDateTime callTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
