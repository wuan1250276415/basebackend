package com.basebackend.security.zerotrust.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 零信任安全配置属性
 *
 * 提供零信任安全模型的配置属性绑定
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Data
@ConfigurationProperties(prefix = "basebackend.security.zerotrust")
public class ZeroTrustProperties {

    /**
     * 是否启用零信任安全
     */
    private boolean enabled = false;

    /**
     * 设备信任配置
     */
    private DeviceConfig device = new DeviceConfig();

    /**
     * 风险评估配置
     */
    private RiskConfig risk = new RiskConfig();

    /**
     * 策略配置
     */
    private PolicyConfig policy = new PolicyConfig();

    /**
     * 监控配置
     */
    private MonitoringConfig monitoring = new MonitoringConfig();

    /**
     * 异步任务配置
     */
    private AsyncConfig async = new AsyncConfig();

    /**
     * 设备信任配置内部类
     */
    @Data
    public static class DeviceConfig {
        /**
         * 是否启用设备指纹收集
         */
        private boolean enabled = true;

        /**
         * 指纹收集超时时间（毫秒）
         */
        private int timeout = 30000;

        /**
         * 设备指纹缓存过期时间（分钟）
         */
        private int cacheExpireMinutes = 60;

        /**
         * 是否启用设备指纹哈希验证
         */
        private boolean enableHashVerification = true;

        /**
         * 是否启用设备指纹持久化存储
         */
        private boolean enablePersistence = false;

        /**
         * 设备指纹数据库表名
         */
        private String tableName = "device_fingerprints";
    }

    /**
     * 风险评估配置内部类
     */
    @Data
    public static class RiskConfig {
        /**
         * 风险阈值（0-100）
         */
        private int threshold = 60;

        /**
         * 高风险阈值（0-100）
         */
        private int highThreshold = 80;

        /**
         * 最大登录尝试次数
         */
        private int maxLoginAttempts = 5;

        /**
         * 账户锁定时长（分钟）
         */
        private long accountLockDuration = 30;

        /**
         * 是否启用实时分析
         */
        private boolean realTimeAnalysisEnabled = true;

        /**
         * 是否启用行为分析
         */
        private boolean behaviorAnalysisEnabled = true;

        /**
         * 是否启用网络分析
         */
        private boolean networkAnalysisEnabled = true;

        /**
         * 风险分析更新间隔（秒）
         */
        private int analysisUpdateInterval = 60;

        /**
         * 风险历史保存天数
         */
        private int historyRetentionDays = 30;

        /**
         * 是否启用机器学习风险预测
         */
        private boolean enableMlPrediction = false;
    }

    /**
     * 策略配置内部类
     */
    @Data
    public static class PolicyConfig {
        /**
         * 信任分数阈值
         */
        private int trustScoreThreshold = 70;

        /**
         * 最大并发会话数
         */
        private int maxConcurrentSessions = 3;

        /**
         * 会话超时时间（分钟）
         */
        private int sessionTimeout = 30;

        /**
         * 是否启用实时监控
         */
        private boolean realTimeMonitoringEnabled = true;

        /**
         * 是否启用策略缓存
         */
        private boolean cacheEnabled = true;

        /**
         * 策略缓存TTL（秒）
         */
        private int cacheTtl = 300;

        /**
         * 是否启用强制模式
         * true: 强制执行策略，false: 仅监控和警告
         */
        private boolean enforceMode = true;

        /**
         * 是否启用审计日志
         */
        private boolean auditEnabled = true;

        /**
         * 策略更新检查间隔（秒）
         */
        private int policyUpdateInterval = 300;

        /**
         * 最小访问时间（小时）
         * 用户访问的最短时间要求
         */
        private int minimumAccessTime = 1;
    }

    /**
     * 监控配置内部类
     */
    @Data
    public static class MonitoringConfig {
        /**
         * 是否启用监控
         */
        private boolean enabled = true;

        /**
         * 监控间隔（分钟）
         */
        private int intervalMinutes = 15;

        /**
         * 是否启用实时威胁检测
         */
        private boolean realTimeThreatDetection = true;

        /**
         * 是否启用行为基线监控
         */
        private boolean behaviorBaselineMonitoring = true;

        /**
         * 监控数据保存天数
         */
        private int dataRetentionDays = 90;

        /**
         * 是否启用监控告警
         */
        private boolean enableAlerts = true;

        /**
         * 告警通知间隔（分钟）
         */
        private int alertIntervalMinutes = 60;

        /**
         * 监控指标收集间隔（秒）
         */
        private int metricsCollectionInterval = 30;
    }

    /**
     * 异步任务配置内部类
     */
    @Data
    public static class AsyncConfig {
        /**
         * 核心线程池大小
         */
        private int corePoolSize = 5;

        /**
         * 最大线程池大小
         */
        private int maxPoolSize = 20;

        /**
         * 队列容量
         */
        private int queueCapacity = 100;

        /**
         * 线程名称前缀
         */
        private String threadNamePrefix = "zerotrust-async-";

        /**
         * 任务执行超时时间（秒）
         */
        private int taskTimeoutSeconds = 300;

        /**
         * 是否启用任务日志
         */
        private boolean enableTaskLogging = true;
    }

    /**
     * 风险等级枚举（配合RiskConfig使用）
     */
    public enum RiskLevel {
        LOW("低风险", 20, "正常放行"),
        MEDIUM("中等风险", 60, "需要额外验证"),
        HIGH("高风险", 80, "限制访问"),
        CRITICAL("严重风险", 100, "拒绝访问");

        private final String description;
        private final int maxScore;
        private final String action;

        RiskLevel(String description, int maxScore, String action) {
            this.description = description;
            this.maxScore = maxScore;
            this.action = action;
        }

        public String getDescription() { return description; }
        public int getMaxScore() { return maxScore; }
        public String getAction() { return action; }

        /**
         * 根据分数获取风险等级
         */
        public static RiskLevel fromScore(int score) {
            for (RiskLevel level : values()) {
                if (score <= level.getMaxScore()) {
                    return level;
                }
            }
            return CRITICAL;
        }
    }

    /**
     * 策略执行模式
     */
    public enum EnforcementMode {
        MONITOR_ONLY("仅监控", false),
        ENFORCE("强制执行", true);

        private final String description;
        private final boolean enforced;

        EnforcementMode(String description, boolean enforced) {
            this.description = description;
            this.enforced = enforced;
        }

        public String getDescription() { return description; }
        public boolean isEnforced() { return enforced; }
    }
}
