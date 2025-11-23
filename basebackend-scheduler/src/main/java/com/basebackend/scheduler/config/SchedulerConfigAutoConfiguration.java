package com.basebackend.scheduler.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 调度器模块自动配置类
 * <p>
 * 负责启用所有调度器相关的配置属性类,并进行配置验证和初始化检查。
 * 通过Spring Boot的自动配置机制,在应用启动时自动加载。
 * </p>
 *
 * <h3>启用的配置属性类:</h3>
 * <ul>
 *   <li>{@link SchedulerProperties} - 调度器全局配置</li>
 *   <li>{@link PowerJobProperties} - PowerJob分布式调度配置</li>
 *   <li>{@link PageProperties} - 分页配置</li>
 * </ul>
 *
 * <h3>配置控制:</h3>
 * <p>
 * 可以通过配置项 {@code scheduler.enabled} 控制调度器模块的启用状态:
 * <ul>
 *   <li>true: 启用调度器模块(默认)</li>
 *   <li>false: 禁用调度器模块,所有配置类不会加载</li>
 * </ul>
 * </p>
 *
 * <h3>配置示例:</h3>
 * <pre>
 * # 启用调度器模块(默认)
 * scheduler:
 *   enabled: true
 *
 * # 禁用调度器模块
 * scheduler:
 *   enabled: false
 * </pre>
 *
 * <h3>配置验证:</h3>
 * <p>
 * 在配置类初始化后,会执行以下验证:
 * <ul>
 *   <li>打印调度器模块启用状态</li>
 *   <li>验证配置属性的有效性</li>
 *   <li>输出关键配置信息(日志)</li>
 * </ul>
 * </p>
 *
 * <h3>自动配置机制:</h3>
 * <p>
 * 该类使用 {@code @AutoConfiguration} 注解,由Spring Boot自动发现和加载。
 * 无需手动在 {@code @Configuration} 类中导入,也无需在 {@code spring.factories} 中声明。
 * </p>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-24
 * @see SchedulerProperties
 * @see PowerJobProperties
 * @see PageProperties
 * @see org.springframework.boot.autoconfigure.AutoConfiguration
 * @see org.springframework.boot.context.properties.EnableConfigurationProperties
 */
@Slf4j
@Configuration
@AutoConfiguration
@EnableConfigurationProperties({
        SchedulerProperties.class,
        PowerJobProperties.class,
        PageProperties.class
})
@ConditionalOnProperty(
        prefix = "scheduler",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SchedulerConfigAutoConfiguration {

    private final SchedulerProperties schedulerProperties;
    private final PowerJobProperties powerJobProperties;
    private final PageProperties pageProperties;

    /**
     * 构造函数 - 注入配置属性类
     *
     * @param schedulerProperties 调度器全局配置
     * @param powerJobProperties  PowerJob配置
     * @param pageProperties      分页配置
     */
    public SchedulerConfigAutoConfiguration(
            SchedulerProperties schedulerProperties,
            PowerJobProperties powerJobProperties,
            PageProperties pageProperties) {
        this.schedulerProperties = schedulerProperties;
        this.powerJobProperties = powerJobProperties;
        this.pageProperties = pageProperties;
    }

    /**
     * 配置初始化 - 在Bean创建后执行
     * <p>
     * 执行配置验证和初始化检查,输出关键配置信息。
     * 如果配置无效,会抛出异常中断应用启动。
     * </p>
     */
    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("调度器模块配置初始化开始");
        log.info("========================================");

        // 验证并打印SchedulerProperties配置
        logSchedulerConfiguration();

        // 验证并打印PowerJobProperties配置
        logPowerJobConfiguration();

        // 验证并打印PageProperties配置
        logPageConfiguration();

        log.info("========================================");
        log.info("调度器模块配置初始化完成");
        log.info("========================================");
    }

    /**
     * 打印并验证调度器全局配置
     */
    private void logSchedulerConfiguration() {
        log.info("【Scheduler全局配置】");
        log.info("  - 模块状态: {}", schedulerProperties.isEnabled() ? "启用" : "禁用");
        log.info("  - 默认租户: {}", schedulerProperties.getDefaultTenantId());
        log.info("  - 时区设置: {}", schedulerProperties.getTimeZone());
        log.info("  - 最大并发工作流: {}", schedulerProperties.getMaxConcurrentWorkflows());
        log.info("  - 最大分发重试: {}", schedulerProperties.getMaxDispatchRetries());
        log.info("  - 重试退避时间: {}ms", schedulerProperties.getDispatchRetryBackoffMillis());
        log.info("  - 幂等键TTL: {}s", schedulerProperties.getIdempotencyKeyTtlSeconds());

        // 执行自定义验证
        try {
            schedulerProperties.validate();
            log.info("  ✓ Scheduler配置验证通过");
        } catch (IllegalArgumentException e) {
            log.error("  ✗ Scheduler配置验证失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 打印并验证PowerJob配置
     */
    private void logPowerJobConfiguration() {
        log.info("【PowerJob配置】");
        log.info("  - 应用名称: {}", powerJobProperties.getAppName());
        log.info("  - 服务器地址: {}", powerJobProperties.getServerAddress());
        log.info("  - Token配置: {}", powerJobProperties.isTokenConfigured() ? "已配置" : "未配置");
        log.info("  - 核心线程数: {}", powerJobProperties.getWorkerCorePoolSize());
        log.info("  - 最大线程数: {}", powerJobProperties.getWorkerMaxPoolSize());
        log.info("  - 心跳间隔: {}ms", powerJobProperties.getHeartbeatIntervalMillis());
        log.info("  - 任务超时: {}s{}",
                powerJobProperties.getMaxTaskExecutionTimeoutSeconds(),
                powerJobProperties.getMaxTaskExecutionTimeoutSeconds() == 0 ? " (不限制)" : "");

        // 执行自定义验证
        try {
            powerJobProperties.validate();
            log.info("  ✓ PowerJob配置验证通过");
        } catch (IllegalArgumentException e) {
            log.error("  ✗ PowerJob配置验证失败: {}", e.getMessage());
            throw e;
        }

        // Token未配置警告(验证通过后输出)
        if (!powerJobProperties.isTokenConfigured()) {
            log.warn("  ⚠ PowerJob Token未配置,如果Server启用了Token验证,Worker将无法连接");
        }
    }

    /**
     * 打印并验证分页配置
     */
    private void logPageConfiguration() {
        log.info("【分页配置】");
        log.info("  - 默认页码: {}", pageProperties.getDefaultPageNo());
        log.info("  - 默认页大小: {}", pageProperties.getDefaultPageSize());
        log.info("  - 最大页大小: {}", pageProperties.getMaxPageSize());
        log.info("  - 最小页大小: {}", pageProperties.getMinPageSize());
        log.info("  - 允许查询全部: {}", pageProperties.getAllowQueryAll() ? "是" : "否");

        // 分页配置没有自定义validate方法,但可以进行基本验证
        if (pageProperties.getMaxPageSize() < pageProperties.getDefaultPageSize()) {
            String errorMsg = String.format(
                    "最大页大小(%d)不能小于默认页大小(%d)",
                    pageProperties.getMaxPageSize(),
                    pageProperties.getDefaultPageSize());
            log.error("  ✗ 分页配置验证失败: {}", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        if (pageProperties.getDefaultPageSize() < pageProperties.getMinPageSize()) {
            String errorMsg = String.format(
                    "默认页大小(%d)不能小于最小页大小(%d)",
                    pageProperties.getDefaultPageSize(),
                    pageProperties.getMinPageSize());
            log.error("  ✗ 分页配置验证失败: {}", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        log.info("  ✓ 分页配置验证通过");
    }
}
