package com.basebackend.backup.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 备份任务实体类
 * 用于定义备份任务的配置和调度信息
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("backup_task")
public class BackupTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称
     */
    @TableField("name")
    private String name;

    /**
     * 数据源类型：mysql/postgres/redis
     */
    @TableField("datasource_type")
    private String datasourceType;

    /**
     * 备份类型：full/incremental
     */
    @TableField("backup_type")
    private String backupType;

    /**
     * CRON表达式，用于定时调度
     */
    @TableField("schedule_cron")
    private String scheduleCron;

    /**
     * 策略配置JSON（包含数据库连接信息、排除表等）
     */
    @TableField("strategy_json")
    private String strategyJson;

    /**
     * 保留策略JSON（保留天数、分层存储策略等）
     */
    @TableField("retention_policy_json")
    private String retentionPolicyJson;

    /**
     * 存储策略JSON（本地路径、云存储配置、多副本策略等）
     */
    @TableField("storage_policy_json")
    private String storagePolicyJson;

    /**
     * 通知策略JSON（失败通知、邮件、钉钉、Slack等）
     */
    @TableField("notify_policy_json")
    private String notifyPolicyJson;

    /**
     * 任务是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 获取任务的分布式锁键
     */
    public String getLockKey() {
        return String.format("backup:lock:%s:%s", datasourceType, id);
    }

    /**
     * 检查是否为增量备份
     */
    public boolean isIncremental() {
        return "incremental".equals(backupType);
    }

    /**
     * 检查是否为全量备份
     */
    public boolean isFull() {
        return "full".equals(backupType);
    }
}
