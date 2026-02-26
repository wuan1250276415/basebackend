package com.basebackend.observability.alert;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 告警事件
 */
@Data
@Builder
public class AlertEvent {

    /**
     * 告警ID
     */
    private Long id;

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 告警级别
     */
    private AlertRule.AlertSeverity severity;

    /**
     * 告警消息
     */
    private String message;

    /**
     * 触发值
     */
    private String triggerValue;

    /**
     * 阈值
     */
    private String thresholdValue;

    /**
     * 告警时间
     */
    private LocalDateTime alertTime;

    /**
     * 额外信息
     */
    private Map<String, Object> metadata;

    /**
     * 通知状态：PENDING, SUCCESS, FAILED
     */
    private NotifyStatus notifyStatus;

    /**
     * 通知渠道
     */
    private String notifyChannels;

    /**
     * 告警状态：TRIGGERED, NOTIFIED, RESOLVED
     */
    private AlertStatus status;

    /**
     * 通知状态枚举
     */
    public enum NotifyStatus {
        PENDING,    // 待发送
        SUCCESS,    // 发送成功
        FAILED      // 发送失败
    }

    /**
     * 告警状态枚举
     */
    public enum AlertStatus {
        TRIGGERED,  // 已触发
        NOTIFIED,   // 已通知
        RESOLVED    // 已解决
    }
}
