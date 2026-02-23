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
 *
 * @author BaseBackend Team
 * @since 2025-01-24
 */
@Slf4j
@Configuration
@AutoConfiguration
@EnableConfigurationProperties({
        SchedulerProperties.class,
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
    private final PageProperties pageProperties;

    public SchedulerConfigAutoConfiguration(
            SchedulerProperties schedulerProperties,
            PageProperties pageProperties) {
        this.schedulerProperties = schedulerProperties;
        this.pageProperties = pageProperties;
    }

    @PostConstruct
    public void init() {
        log.info("调度器模块配置初始化开始");

        logSchedulerConfiguration();
        logPageConfiguration();

        log.info("调度器模块配置初始化完成");
    }

    private void logSchedulerConfiguration() {
        log.info("【Scheduler全局配置】");
        log.info("  - 模块状态: {}", schedulerProperties.isEnabled() ? "启用" : "禁用");
        log.info("  - 默认租户: {}", schedulerProperties.getDefaultTenantId());
        log.info("  - 时区设置: {}", schedulerProperties.getTimeZone());
        log.info("  - 最大并发工作流: {}", schedulerProperties.getMaxConcurrentWorkflows());

        try {
            schedulerProperties.validate();
        } catch (IllegalArgumentException e) {
            log.error("Scheduler配置验证失败: {}", e.getMessage());
            throw e;
        }
    }

    private void logPageConfiguration() {
        log.info("【分页配置】");
        log.info("  - 默认页大小: {}", pageProperties.getDefaultPageSize());
        log.info("  - 最大页大小: {}", pageProperties.getMaxPageSize());

        if (pageProperties.getMaxPageSize() < pageProperties.getDefaultPageSize()) {
            String errorMsg = String.format(
                    "最大页大小(%d)不能小于默认页大小(%d)",
                    pageProperties.getMaxPageSize(),
                    pageProperties.getDefaultPageSize());
            throw new IllegalArgumentException(errorMsg);
        }

        if (pageProperties.getDefaultPageSize() < pageProperties.getMinPageSize()) {
            String errorMsg = String.format(
                    "默认页大小(%d)不能小于最小页大小(%d)",
                    pageProperties.getDefaultPageSize(),
                    pageProperties.getMinPageSize());
            throw new IllegalArgumentException(errorMsg);
        }
    }
}
