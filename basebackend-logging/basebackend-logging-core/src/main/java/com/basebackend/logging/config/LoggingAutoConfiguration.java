package com.basebackend.logging.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 日志模块顶层自动配置入口
 *
 * 各子模块（audit / advanced / monitoring）已拆分为独立模块，
 * 通过各自的 AutoConfiguration.imports 自行注册，不再由此处集中 @Import。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(LoggingUnifiedProperties.class)
@ConditionalOnProperty(value = "basebackend.logging.enabled", matchIfMissing = true)
public class LoggingAutoConfiguration {

    public LoggingAutoConfiguration() {
        log.info("日志模块自动配置已加载");
    }
}
