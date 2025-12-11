package com.basebackend.scheduler.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * PowerJob Worker自动配置类
 * <p>
 * 负责初始化和配置PowerJob Worker的基础设施。
 * 注意：具体的Worker实例化由powerjob-worker-spring-boot-starter自动处理。
 * </p>
 *
 * <h3>主要职责:</h3>
 * <ul>
 *   <li>条件化配置激活</li>
 *   <li>配置属性加载</li>
 *   <li>初始化日志记录</li>
 *   <li>健康检查支持</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-11-25
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = {
    "tech.powerjob.worker.Worker",
    "tech.powerjob.worker.WorkerConfig"
})
@EnableConfigurationProperties({PowerJobProperties.class, JobConfigProperties.class})
@ConditionalOnProperty(prefix = "scheduler.powerjob", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class PowerJobAutoConfiguration {

    private final PowerJobProperties powerJobProperties;
    private final JobConfigProperties jobConfigProperties;

    @PostConstruct
    public void init() {
        log.info("[PowerJob] AutoConfiguration initialized");
        log.info("[PowerJob] AppName: {}", powerJobProperties.getAppName());
        log.info("[PowerJob] ServerAddress: {}", powerJobProperties.getServerAddress());
        log.info("[PowerJob] Worker Core Pool Size: {}", powerJobProperties.getWorkerCorePoolSize());
        log.info("[PowerJob] Worker Max Pool Size: {}", powerJobProperties.getWorkerMaxPoolSize());
        log.info("[PowerJob] Heartbeat Interval: {}ms", powerJobProperties.getHeartbeatIntervalMillis());
        log.info("[PowerJob] Task Auto-Register: {}", jobConfigProperties.isAutoRegister());
    }
}
