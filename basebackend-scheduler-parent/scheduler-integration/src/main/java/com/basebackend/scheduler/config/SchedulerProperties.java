package com.basebackend.scheduler.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 调度器模块全局配置属性
 * <p>
 * 提供调度器模块的核心配置管理,支持从Nacos配置中心动态刷新。
 * 配置前缀: scheduler
 * </p>
 *
 * <h3>配置项分类:</h3>
 * <ul>
 *   <li><b>基础配置</b>: enabled(模块开关)、defaultTenantId(默认租户)、timeZone(时区)</li>
 *   <li><b>并发控制</b>: maxConcurrentWorkflows(最大并发工作流)、maxDispatchRetries(重试次数)</li>
 *   <li><b>幂等性配置</b>: idempotencyKeyTtlSeconds(幂等键TTL)</li>
 * </ul>
 *
 * <h3>使用示例:</h3>
 * <pre>{@code
 * @Autowired
 * private SchedulerProperties schedulerProperties;
 *
 * public void example() {
 *     // 获取配置值
 *     boolean enabled = schedulerProperties.isEnabled();
 *     String tenantId = schedulerProperties.getDefaultTenantId();
 *     int maxWorkflows = schedulerProperties.getMaxConcurrentWorkflows();
 *
 *     // 配置会自动从Nacos刷新(@RefreshScope)
 * }
 * }</pre>
 *
 * <h3>配置文件示例(application.yml):</h3>
 * <pre>
 * scheduler:
 *   enabled: true
 *   default-tenant-id: default
 *   time-zone: UTC
 *   max-concurrent-workflows: 10
 *   max-dispatch-retries: 3
 *   dispatch-retry-backoff-millis: 500
 *   idempotency-key-ttl-seconds: 900
 * </pre>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-24
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 * @see org.springframework.cloud.context.config.annotation.RefreshScope
 */
@Data
@Component
@Validated
@RefreshScope
@ConfigurationProperties(prefix = "scheduler")
public class SchedulerProperties {

    // ========== 基础配置 ==========

    /**
     * 调度器模块总开关
     * <p>
     * 控制整个调度器模块是否启用。关闭后,所有调度功能将被禁用。
     * </p>
     * <p>
     * 默认值: true
     * </p>
     */
    private boolean enabled = true;

    /**
     * 默认租户ID
     * <p>
     * 用于多租户隔离场景,未指定租户时使用此默认值。
     * 租户ID用于隔离不同客户的任务和工作流。
     * </p>
     * <p>
     * 默认值: "default"
     * </p>
     * <p>
     * 注意: 租户ID不能为空,必须是有效的标识符
     * </p>
     */
    @NotBlank(message = "默认租户ID不能为空")
    private String defaultTenantId = "default";

    /**
     * 时区设置
     * <p>
     * 影响任务调度时间计算和日志时间戳。
     * 建议生产环境使用"UTC"标准时间,开发环境可使用本地时区(如"Asia/Shanghai")。
     * </p>
     * <p>
     * 常用时区:
     * <ul>
     *   <li>UTC: 协调世界时(推荐)</li>
     *   <li>Asia/Shanghai: 东八区(GMT+8)</li>
     *   <li>America/New_York: 美国东部时间</li>
     * </ul>
     * </p>
     * <p>
     * 默认值: "UTC"
     * </p>
     */
    @NotBlank(message = "时区设置不能为空")
    private String timeZone = "UTC";

    // ========== 并发控制 ==========

    /**
     * 最大并发工作流数
     * <p>
     * 限制同时运行的工作流数量,防止资源耗尽。
     * 超过此限制的新工作流将被排队或拒绝(取决于策略)。
     * </p>
     * <p>
     * 推荐值:
     * <ul>
     *   <li>小型应用: 5-10</li>
     *   <li>中型应用: 10-50</li>
     *   <li>大型应用: 50-200</li>
     * </ul>
     * </p>
     * <p>
     * 默认值: 10
     * </p>
     */
    @Min(value = 1, message = "最大并发工作流数必须大于等于1")
    private int maxConcurrentWorkflows = 10;

    /**
     * 最大任务分发重试次数
     * <p>
     * 任务分发失败时的最大重试次数。
     * 0表示不重试,建议设置为3-5次。
     * </p>
     * <p>
     * 默认值: 3
     * </p>
     */
    @Min(value = 0, message = "最大任务分发重试次数必须大于等于0")
    private int maxDispatchRetries = 3;

    /**
     * 任务分发重试退避时间(毫秒)
     * <p>
     * 两次重试之间的等待时间。
     * 建议使用指数退避策略,此值为初始退避时间。
     * </p>
     * <p>
     * 示例:
     * <ul>
     *   <li>第1次重试: 500ms</li>
     *   <li>第2次重试: 1000ms(2倍)</li>
     *   <li>第3次重试: 2000ms(4倍)</li>
     * </ul>
     * </p>
     * <p>
     * 默认值: 500(毫秒)
     * </p>
     */
    @Min(value = 100, message = "任务分发重试退避时间必须大于等于100毫秒")
    private long dispatchRetryBackoffMillis = 500L;

    // ========== 幂等性配置 ==========

    /**
     * 幂等性键TTL(秒)
     * <p>
     * 幂等性键在Redis中的存活时间。
     * 用于防止任务重复执行,超过TTL后幂等性保护失效。
     * </p>
     * <p>
     * 推荐值:
     * <ul>
     *   <li>短任务(秒级): 60-300秒</li>
     *   <li>中等任务(分钟级): 300-900秒</li>
     *   <li>长任务(小时级): 900-3600秒</li>
     * </ul>
     * </p>
     * <p>
     * 默认值: 900(15分钟)
     * </p>
     */
    @Min(value = 1, message = "幂等性键TTL必须大于等于1秒")
    private long idempotencyKeyTtlSeconds = 900L;

    /**
     * 验证配置的有效性
     * <p>
     * Spring Boot会在配置绑定后自动调用此方法(通过@Validated)。
     * 这里可以添加自定义的跨字段验证逻辑。
     * </p>
     *
     * @throws IllegalArgumentException 如果配置无效
     */
    public void validate() {
        // 验证时区格式是否有效
        try {
            java.util.TimeZone.getTimeZone(timeZone);
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的时区设置: " + timeZone, e);
        }

        // 验证并发数和重试配置的合理性
        if (maxConcurrentWorkflows < 1) {
            throw new IllegalArgumentException("最大并发工作流数必须大于0");
        }

        if (maxDispatchRetries < 0) {
            throw new IllegalArgumentException("最大任务分发重试次数不能为负数");
        }

        if (dispatchRetryBackoffMillis < 100) {
            throw new IllegalArgumentException("任务分发重试退避时间不能小于100毫秒");
        }
    }
}
