package com.basebackend.scheduler.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Scheduler核心模块配置属性。
 * 支持通过配置文件或环境变量自定义缓存、线程池等参数。
 */
@Data
@ConfigurationProperties(prefix = "scheduler.core")
public class SchedulerCoreProperties {

    /**
     * 缓存配置
     */
    private CacheConfig cache = new CacheConfig();

    /**
     * 线程池配置
     */
    private ThreadPoolConfig threadPool = new ThreadPoolConfig();

    /**
     * 断路器配置
     */
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();

    /**
     * 缓存配置类
     */
    @Data
    public static class CacheConfig {
        /**
         * 工作流定义缓存最大容量
         */
        private int definitionMaxSize = 1000;

        /**
         * 工作流定义缓存过期时间
         */
        private Duration definitionExpireAfterAccess = Duration.ofHours(1);

        /**
         * 工作流实例缓存最大容量
         */
        private int instanceMaxSize = 10000;

        /**
         * 工作流实例缓存过期时间
         */
        private Duration instanceExpireAfterAccess = Duration.ofMinutes(30);

        /**
         * 处理器缓存最大容量
         */
        private int processorMaxSize = 200;

        /**
         * 处理器缓存过期时间
         */
        private Duration processorExpireAfterAccess = Duration.ofHours(1);

        /**
         * 性能指标缓存最大容量
         */
        private int metricsMaxSize = 500;

        /**
         * 性能指标缓存过期时间
         */
        private Duration metricsExpireAfterAccess = Duration.ofMinutes(10);
    }

    /**
     * 线程池配置类
     */
    @Data
    public static class ThreadPoolConfig {
        /**
         * 工作流执行器核心线程数
         */
        private int workflowCorePoolSize = 8;

        /**
         * 工作流执行器最大线程数
         */
        private int workflowMaxPoolSize = 32;

        /**
         * 工作流执行器队列容量
         */
        private int workflowQueueCapacity = 200;

        /**
         * 重试执行器核心线程数
         */
        private int retryCorePoolSize = 4;

        /**
         * 重试执行器最大线程数
         */
        private int retryMaxPoolSize = 16;

        /**
         * 重试执行器队列容量
         */
        private int retryQueueCapacity = 100;

        /**
         * 线程空闲存活时间（秒）
         */
        private long keepAliveSeconds = 60;
    }

    /**
     * 断路器配置类
     */
    @Data
    public static class CircuitBreakerConfig {
        /**
         * 是否启用断路器
         */
        private boolean enabled = true;

        /**
         * 失败率阈值（百分比），超过此值断路器打开
         */
        private int failureRateThreshold = 50;

        /**
         * 慢调用率阈值（百分比）
         */
        private int slowCallRateThreshold = 80;

        /**
         * 慢调用时长阈值
         */
        private Duration slowCallDurationThreshold = Duration.ofSeconds(5);

        /**
         * 滑动窗口大小（调用次数）
         */
        private int slidingWindowSize = 100;

        /**
         * 最小调用次数（达到此数量后才计算失败率）
         */
        private int minimumNumberOfCalls = 10;

        /**
         * 断路器打开后等待时间
         */
        private Duration waitDurationInOpenState = Duration.ofSeconds(30);

        /**
         * 半开状态允许的调用次数
         */
        private int permittedNumberOfCallsInHalfOpenState = 5;
    }
}
