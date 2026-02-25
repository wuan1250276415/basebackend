package com.basebackend.logging.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 日志管道自动配置
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(LogPipelineProperties.class)
@ConditionalOnProperty(value = "basebackend.logging.pipeline.enabled", havingValue = "true")
public class LogPipelineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LogTransport logTransport(LogPipelineProperties properties) {
        String type = properties.getTransport();
        log.info("初始化日志传输后端: {}", type);
        return new ConsoleLogTransport();
    }

    @Bean
    @ConditionalOnMissingBean
    public LogPipelineChain logPipelineChain(LogTransport transport,
                                              ObjectProvider<List<LogPipelineProcessor>> processorsProvider) {
        List<LogPipelineProcessor> processors = processorsProvider.getIfAvailable();
        log.info("初始化日志管道链，处理器数量: {}", processors != null ? processors.size() : 0);
        return new LogPipelineChain(processors, transport);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "basebackend.logging.pipeline.wal.enabled", havingValue = "true")
    public LocalWalBuffer localWalBuffer(LogPipelineProperties properties) {
        LogPipelineProperties.WalConfig wal = properties.getWal();
        log.info("初始化本地 WAL 缓冲，目录: {}", wal.getDirectory());
        return new LocalWalBuffer(wal.getDirectory(), wal.getMaxFileSizeBytes(), wal.getMaxFiles());
    }
}
