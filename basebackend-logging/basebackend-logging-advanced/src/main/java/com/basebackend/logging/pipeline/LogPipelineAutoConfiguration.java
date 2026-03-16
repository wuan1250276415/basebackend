package com.basebackend.logging.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 日志管道自动配置
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(LogPipelineProperties.class)
@ConditionalOnProperty(value = "basebackend.logging.pipeline.enabled", havingValue = "true")
public class LogPipelineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LogTransport logTransport(LogPipelineProperties properties) {
        String type = properties.getTransport();
        log.info("初始化日志传输后端: {}", type);
        return switch (type == null ? "console" : type.toLowerCase(java.util.Locale.ROOT)) {
            case "console", "" -> new ConsoleLogTransport();
            // 其他传输类型（loki、kafka、file 等）依赖额外组件，
            // 如需支持请在宿主服务中通过 @Bean 覆盖此 @ConditionalOnMissingBean，
            // 或引入对应的扩展模块并注册相应的 LogTransport 实现。
            default -> {
                log.warn("不支持的日志传输类型 '{}'，回退到 ConsoleLogTransport。"
                        + " 如需其他传输，请自定义 LogTransport Bean。", type);
                yield new ConsoleLogTransport();
            }
        };
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
