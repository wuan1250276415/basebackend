package com.basebackend.messaging.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Webhook事件模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件数据
     */
    private Object data;

    /**
     * 事件发生时间
     */
    private LocalDateTime timestamp;

    /**
     * 事件来源
     */
    private String source;

    /**
     * 追踪ID
     */
    private String traceId;

    /**
     * 额外的元数据
     */
    private Map<String, Object> metadata;
}
