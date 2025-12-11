package com.basebackend.scheduler.camunda.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Camunda 工作流引擎自定义配置属性
 *
 * <p>用于扩展 Camunda 默认配置，提供缓存、监控、审计等企业级特性的开关。
 * 配置前缀：{@code camunda.custom}
 *
 * <p>配置示例：
 * <pre>
 * camunda:
 *   custom:
 *     cache-enabled: true
 *     max-cache-size: 1000
 *     monitoring-enabled: true
 *     audit-enabled: true
 * </pre>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Component
@ConfigurationProperties(prefix = "camunda.custom")
public class CamundaProperties {

    /**
     * 是否启用工作流定义/部署缓存
     *
     * <p>启用后将使用 Caffeine 缓存流程定义、部署信息等，提升查询性能。
     * 默认值：{@code true}
     */
    private boolean cacheEnabled = true;

    /**
     * 工作流缓存最大数量
     *
     * <p>限制缓存中流程定义、部署、表单等工作流构件的总数量。
     * 超过此数量后，将按 LRU 策略淘汰旧数据。
     * 默认值：{@code 1000}
     */
    private int maxCacheSize = 1000;

    /**
     * 是否启用工作流监控
     *
     * <p>启用后将暴露 Micrometer 指标、健康检查端点等监控特性。
     * 默认值：{@code true}
     */
    private boolean monitoringEnabled = true;

    /**
     * 是否启用审计日志
     *
     * <p>启用后将记录流程启动、审批、完成等关键操作的审计日志。
     * 审计日志将发送到统一的审计系统（如 ELK、OTel 等）。
     * 默认值：{@code true}
     */
    private boolean auditEnabled = true;

    /**
     * 缓存过期时间（分钟）
     *
     * <p>流程定义缓存的过期时间，超过此时间后缓存将失效。
     * 默认值：{@code 30} 分钟
     */
    private int cacheExpireMinutes = 30;
}
