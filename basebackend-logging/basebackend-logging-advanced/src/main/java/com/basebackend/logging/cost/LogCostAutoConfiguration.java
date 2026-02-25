package com.basebackend.logging.cost;

import ch.qos.logback.classic.LoggerContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 日志成本治理自动配置
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(LogCostProperties.class)
@ConditionalOnProperty(value = "basebackend.logging.cost.enabled", havingValue = "true")
public class LogCostAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LogVolumeTracker logVolumeTracker(LogCostProperties properties) {
        log.info("初始化日志量跟踪器，窗口: {}s", properties.getWindowSeconds());
        return new LogVolumeTracker(properties.getWindowSeconds());
    }

    @Bean
    @ConditionalOnMissingBean
    public AdaptiveSamplerFilter adaptiveSamplerFilter(LogVolumeTracker tracker,
                                                        LogCostProperties properties) {
        AdaptiveSamplerFilter filter = new AdaptiveSamplerFilter(
                tracker,
                properties.getEventThreshold(),
                properties.getByteThreshold(),
                properties.getSamplingRate(),
                properties.isExemptHighSeverity());
        filter.setName("adaptiveSamplerFilter");

        if (LoggerFactory.getILoggerFactory() instanceof LoggerContext loggerContext) {
            loggerContext.addTurboFilter(filter);
            log.info("自适应采样过滤器已注册到 Logback LoggerContext，阈值: {} events / {} bytes",
                    properties.getEventThreshold(), properties.getByteThreshold());
        } else {
            log.warn("当前日志实现非 Logback，自适应采样过滤器未注册");
        }

        return filter;
    }

    @Bean
    @ConditionalOnMissingBean
    public LogCostEndpoint logCostEndpoint(LogVolumeTracker tracker,
                                            LogCostProperties properties) {
        log.info("初始化日志成本治理 Actuator 端点");
        return new LogCostEndpoint(tracker, properties);
    }
}
