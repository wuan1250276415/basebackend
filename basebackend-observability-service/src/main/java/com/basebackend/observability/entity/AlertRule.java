package com.basebackend.observability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警规则实体
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Data
@TableName("alert_rule")
public class AlertRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 指标名称
     */
    private String metricName;

    /**
     * 阈值
     */
    private Double threshold;

    /**
     * 操作符：gt(大于), lt(小于), eq(等于), gte(大于等于), lte(小于等于)
     */
    private String operator;

    /**
     * 持续时间（秒）
     */
    private Integer duration;

    /**
     * 告警级别：info, warning, error, critical
     */
    private String severity;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 通知渠道：email, sms, webhook
     */
    private String notificationChannels;

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
    private String createdBy;

    /**
     * 逻辑删除
     */
    private Integer deleted;
}
