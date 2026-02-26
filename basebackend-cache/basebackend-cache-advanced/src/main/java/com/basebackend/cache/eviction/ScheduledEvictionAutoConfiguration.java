package com.basebackend.cache.eviction;

import com.basebackend.cache.config.CacheProperties;
import com.basebackend.cache.manager.CacheEvictionManager;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 定时缓存淘汰自动配置
 * 当配置了定时淘汰规则时自动注册
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "basebackend.cache.eviction", name = "enabled", havingValue = "true")
public class ScheduledEvictionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "cacheEvictionTaskScheduler")
    public TaskScheduler cacheEvictionTaskScheduler(CacheProperties cacheProperties) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(cacheProperties.getEviction().getPoolSize());
        scheduler.setThreadNamePrefix("cache-eviction-");
        scheduler.setDaemon(true);
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        scheduler.initialize();
        log.info("Created cache eviction TaskScheduler with pool size: {}",
                cacheProperties.getEviction().getPoolSize());
        return scheduler;
    }

    @Bean
    @ConditionalOnMissingBean
    public ScheduledEvictionExecutor scheduledEvictionExecutor(
            CacheEvictionManager cacheEvictionManager,
            TaskScheduler cacheEvictionTaskScheduler,
            CacheProperties cacheProperties,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        log.info("Registering ScheduledEvictionExecutor with {} rules",
                cacheProperties.getEviction().getScheduled().size());
        return new ScheduledEvictionExecutor(
                cacheEvictionManager,
                cacheEvictionTaskScheduler,
                cacheProperties.getEviction().getScheduled(),
                meterRegistry);
    }
}
