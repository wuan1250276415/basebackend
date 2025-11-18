package com.basebackend.observability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警事件实体
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Data
@TableName("alert_event")
public class AlertEvent {

    @TableId(type = IdType.AUTO)
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
     * 指标名称
     */
    private String metricName;

    /**
     * 当前值
     */
    private Double currentValue;

    /**
     * 阈值
     */
    private Double threshold;

    /**
     * 告警级别
     */
    private String severity;

    /**
     * 告警消息
     */
    private String message;

    /**
     * 告警状态：firing, resolved
     */
    private String status;

    /**
     * 触发时间
     */
    private LocalDateTime firedAt;

    /**
     * 解决时间
     */
    private LocalDateTime resolvedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
