package com.basebackend.logging.loglevel;

import com.alibaba.nacos.api.config.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 动态日志级别自动配置
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(DynamicLogLevelProperties.class)
@ConditionalOnProperty(value = "basebackend.logging.log-level.enabled", matchIfMissing = true)
public class DynamicLogLevelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LogLevelManager logLevelManager(LoggingSystem loggingSystem,
                                            DynamicLogLevelProperties properties) {
        log.info("初始化动态日志级别管理器");
        return new LogLevelManager(loggingSystem, properties.getMaxTtlSeconds());
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicLogLevelEndpoint dynamicLogLevelEndpoint(LogLevelManager logLevelManager,
                                                            DynamicLogLevelProperties properties) {
        log.info("初始化动态日志级别 Actuator 端点");
        return new DynamicLogLevelEndpoint(logLevelManager, properties.getDefaultTtlSeconds());
    }

    /**
     * Nacos 集成（仅在 Nacos ConfigService 可用时激活）
     */
    @Configuration
    @ConditionalOnClass(ConfigService.class)
    @ConditionalOnProperty(value = "basebackend.logging.log-level.nacos.enabled", havingValue = "true")
    static class NacosLogLevelConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public NacosLogLevelListener nacosLogLevelListener(ConfigService configService,
                                                            LogLevelManager logLevelManager,
                                                            DynamicLogLevelProperties properties) {
            DynamicLogLevelProperties.NacosIntegration nacos = properties.getNacos();
            log.info("初始化 Nacos 日志级别监听: dataId={}, group={}", nacos.getDataId(), nacos.getGroup());
            return new NacosLogLevelListener(configService, logLevelManager,
                    nacos.getDataId(), nacos.getGroup());
        }
    }
}
