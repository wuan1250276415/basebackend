package com.basebackend.observability.alert;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警规则实体
 */
@Data
public class AlertRule {

    /**
     * 规则ID
     */
    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则类型：THRESHOLD（阈值）, LOG（日志）, CUSTOM（自定义）
     */
    private AlertRuleType ruleType;

    /**
     * 指标名称（用于 THRESHOLD 类型）
     */
    private String metricName;

    /**
     * 阈值
     */
    private Double thresholdValue;

    /**
     * 比较运算符：>, <, >=, <=, ==
     */
    private String comparisonOperator;

    /**
     * 持续时间（秒）- 指标需要持续多久才触发告警
     */
    private Integer durationSeconds;

    /**
     * 严重程度：INFO, WARNING, ERROR, CRITICAL
     */
    private AlertSeverity severity;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 通知渠道：email, dingtalk, wechat（逗号分隔）
     */
    private String notifyChannels;

    /**
     * 规则描述
     */
    private String description;

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
     * 更新人
     */
    private Long updateBy;

    /**
     * 是否删除
     */
    private Boolean deleted;

    /**
     * 规则类型枚举
     */
    public enum AlertRuleType {
        THRESHOLD,  // 阈值告警
        LOG,        // 日志告警
        CUSTOM      // 自定义告警
    }

    /**
     * 告警级别枚举
     */
    public enum AlertSeverity {
        INFO,       // 信息
        WARNING,    // 警告
        ERROR,      // 错误
        CRITICAL    // 严重
    }
}
