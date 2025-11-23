package com.basebackend.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 缓存配置属性类
 * 支持多级缓存、指标、预热、序列化、容错等配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "basebackend.cache")
public class CacheProperties {

    /**
     * 是否启用缓存模块
     */
    private boolean enabled = true;

    /**
     * 多级缓存配置
     */
    private MultiLevel multiLevel = new MultiLevel();

    /**
     * 缓存指标配置
     */
    private Metrics metrics = new Metrics();

    /**
     * 缓存预热配置
     */
    private Warming warming = new Warming();

    /**
     * 序列化配置
     */
    private Serialization serialization = new Serialization();

    /**
     * 容错和降级配置
     */
    private Resilience resilience = new Resilience();

    /**
     * 缓存键配置
     */
    private Key key = new Key();

    /**
     * 缓存模板配置
     */
    private Template template = new Template();

    /**
     * 分布式锁配置
     */
    private Lock lock = new Lock();

    /**
     * 多级缓存配置
     */
    @Data
    public static class MultiLevel {
        /**
         * 是否启用多级缓存（本地缓存 + Redis）
         */
        private boolean enabled = false;

        /**
         * 本地缓存最大条目数
         */
        private int localMaxSize = 1000;

        /**
         * 本地缓存默认过期时间
         */
        private Duration localTtl = Duration.ofMinutes(5);

        /**
         * 淘汰策略：LRU, LFU, FIFO
         */
        private String evictionPolicy = "LRU";
    }

    /**
     * 缓存指标配置
     */
    @Data
    public static class Metrics {
        /**
         * 是否启用指标收集
         */
        private boolean enabled = true;

        /**
         * 低命中率阈值（触发警告日志）
         */
        private double lowHitRateThreshold = 0.5;

        /**
         * 指标导出到 Micrometer
         */
        private boolean exportToMicrometer = true;
    }

    /**
     * 缓存预热配置
     */
    @Data
    public static class Warming {
        /**
         * 是否启用缓存预热
         */
        private boolean enabled = false;

        /**
         * 预热任务列表
         */
        private List<WarmingTask> tasks = new ArrayList<>();

        /**
         * 预热超时时间
         */
        private Duration timeout = Duration.ofMinutes(5);

        /**
         * 是否异步预热
         */
        private boolean async = true;
    }

    /**
     * 预热任务配置
     */
    @Data
    public static class WarmingTask {
        /**
         * 任务名称
         */
        private String name;

        /**
         * 优先级（数字越小优先级越高）
         */
        private int priority = 0;

        /**
         * 缓存名称
         */
        private String cacheName;

        /**
         * TTL
         */
        private Duration ttl = Duration.ofHours(1);
    }

    /**
     * 序列化配置
     */
    @Data
    public static class Serialization {
        /**
         * 序列化类型：json, protobuf, kryo
         */
        private String type = "json";

        /**
         * JSON 序列化配置
         */
        private Json json = new Json();

        /**
         * Protobuf 序列化配置
         */
        private Protobuf protobuf = new Protobuf();

        /**
         * Kryo 序列化配置
         */
        private Kryo kryo = new Kryo();

        @Data
        public static class Json {
            /**
             * 是否格式化输出
             */
            private boolean prettyPrint = false;

            /**
             * 是否包含类型信息
             */
            private boolean includeTypeInfo = false;
        }

        @Data
        public static class Protobuf {
            /**
             * 是否启用
             */
            private boolean enabled = false;
        }

        @Data
        public static class Kryo {
            /**
             * 是否启用
             */
            private boolean enabled = false;

            /**
             * 是否注册所有类
             */
            private boolean registerRequired = false;
        }
    }

    /**
     * 容错和降级配置
     */
    @Data
    public static class Resilience {
        /**
         * 是否启用降级（Redis 故障时降级到本地缓存或直接访问数据源）
         */
        private boolean fallbackEnabled = true;

        /**
         * Redis 操作超时时间
         */
        private Duration timeout = Duration.ofSeconds(3);

        /**
         * 熔断器配置
         */
        private CircuitBreaker circuitBreaker = new CircuitBreaker();

        /**
         * 自动恢复检测
         */
        private AutoRecovery autoRecovery = new AutoRecovery();

        @Data
        public static class CircuitBreaker {
            /**
             * 是否启用熔断器
             */
            private boolean enabled = false;

            /**
             * 连续失败次数阈值
             */
            private int failureThreshold = 5;

            /**
             * 熔断器打开时长
             */
            private Duration openDuration = Duration.ofSeconds(30);

            /**
             * 半开状态允许的请求数
             */
            private int halfOpenRequests = 3;
        }

        @Data
        public static class AutoRecovery {
            /**
             * 是否启用自动恢复检测
             */
            private boolean enabled = true;

            /**
             * 检测间隔
             */
            private Duration checkInterval = Duration.ofSeconds(10);
        }
    }

    /**
     * 缓存键配置
     */
    @Data
    public static class Key {
        /**
         * 键前缀（用于命名空间隔离）
         */
        private String prefix = "basebackend";

        /**
         * 键分隔符
         */
        private String separator = ":";

        /**
         * 是否包含应用名
         */
        private boolean includeAppName = true;
    }

    /**
     * 缓存模板配置
     */
    @Data
    public static class Template {
        /**
         * Cache-Aside 模式配置
         */
        private CacheAside cacheAside = new CacheAside();

        /**
         * Write-Through 模式配置
         */
        private WriteThrough writeThrough = new WriteThrough();

        /**
         * Write-Behind 模式配置
         */
        private WriteBehind writeBehind = new WriteBehind();

        @Data
        public static class CacheAside {
            /**
             * 默认 TTL
             */
            private Duration defaultTtl = Duration.ofHours(1);

            /**
             * 是否启用布隆过滤器防止缓存穿透
             */
            private boolean bloomFilterEnabled = false;
        }

        @Data
        public static class WriteThrough {
            /**
             * 是否启用
             */
            private boolean enabled = false;
        }

        @Data
        public static class WriteBehind {
            /**
             * 是否启用
             */
            private boolean enabled = false;

            /**
             * 批量写入大小
             */
            private int batchSize = 100;

            /**
             * 批量写入间隔
             */
            private Duration batchInterval = Duration.ofSeconds(5);
        }
    }

    /**
     * 分布式锁配置
     */
    @Data
    public static class Lock {
        /**
         * 默认等待时间
         */
        private Duration defaultWaitTime = Duration.ofSeconds(10);

        /**
         * 默认租约时间
         */
        private Duration defaultLeaseTime = Duration.ofSeconds(30);

        /**
         * 是否启用公平锁
         */
        private boolean fairLockEnabled = false;

        /**
         * 是否启用红锁
         */
        private boolean redLockEnabled = false;
    }
}
