package com.basebackend.scheduler.model;

import com.basebackend.scheduler.enums.ExecuteType;
import com.basebackend.scheduler.enums.JobType;
import com.basebackend.scheduler.enums.TimeExpressionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务信息模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private Long id;

    /**
     * PowerJob任务ID
     */
    private Long powerJobId;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务类型
     */
    private JobType jobType;

    /**
     * 执行类型
     */
    private ExecuteType executeType;

    /**
     * 时间表达式类型
     */
    private TimeExpressionType timeExpressionType;

    /**
     * 时间表达式
     * CRON: 0 0 0 * * ?
     * FIXED_RATE: 5000 (毫秒)
     * FIXED_DELAY: 5000 (毫秒)
     */
    private String timeExpression;

    /**
     * 处理器类名
     * 例如: com.basebackend.scheduler.handler.OrderTimeoutHandler
     */
    private String processorType;

    /**
     * 任务参数(JSON格式)
     */
    private String jobParams;

    /**
     * 最大执行时间(秒)
     */
    private Integer maxInstanceNum;

    /**
     * 最大重试次数
     */
    private Integer maxRetryTimes;

    /**
     * 重试间隔(秒)
     */
    private Integer retryInterval;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 告警配置(JSON格式)
     * {
     *   "dingtalk": "webhook_url",
     *   "email": "admin@example.com"
     * }
     */
    private String alertConfig;

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
    private String createBy;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 应用ID(多租户)
     */
    private Long appId;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 解析任务参数为Map
     */
    public Map<String, Object> getJobParamsMap() {
        if (jobParams == null || jobParams.isEmpty()) {
            return Map.of();
        }
        return com.alibaba.fastjson2.JSON.parseObject(jobParams, Map.class);
    }

    /**
     * 解析告警配置为Map
     */
    public Map<String, String> getAlertConfigMap() {
        if (alertConfig == null || alertConfig.isEmpty()) {
            return Map.of();
        }
        return com.alibaba.fastjson2.JSON.parseObject(alertConfig, Map.class);
    }
}
