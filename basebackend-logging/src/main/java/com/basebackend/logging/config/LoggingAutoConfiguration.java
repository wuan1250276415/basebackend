package com.basebackend.logging.config;

import com.basebackend.logging.audit.config.AuditAutoConfiguration;
import com.basebackend.logging.cache.HotLogCacheConfiguration;
import com.basebackend.logging.masking.MaskingAutoConfiguration;
import com.basebackend.logging.statistics.config.StatisticsAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 日志模块顶层自动配置入口
 *
 * 统一导入所有子配置类，通过 {@code basebackend.logging.enabled} 控制整体开关。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(LoggingUnifiedProperties.class)
@ConditionalOnProperty(value = "basebackend.logging.enabled", matchIfMissing = true)
@Import({
        AuditAutoConfiguration.class,
        MaskingAutoConfiguration.class,
        HotLogCacheConfiguration.class,
        StatisticsAutoConfiguration.class
})
public class LoggingAutoConfiguration {

    public LoggingAutoConfiguration() {
        log.info("日志模块自动配置已加载");
    }
}
