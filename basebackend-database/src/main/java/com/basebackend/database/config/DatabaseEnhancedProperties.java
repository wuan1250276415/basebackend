package com.basebackend.database.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库增强配置属性
 * Note: This is registered via @EnableConfigurationProperties in DatabaseEnhancedAutoConfiguration
 */
@Data
@ConfigurationProperties(prefix = "database.enhanced")
public class DatabaseEnhancedProperties {

    /**
     * 审计系统配置
     */
    private AuditProperties audit = new AuditProperties();

    /**
     * 多租户配置
     */
    private MultiTenancyProperties multiTenancy = new MultiTenancyProperties();

    /**
     * 数据安全配置
     */
    private SecurityProperties security = new SecurityProperties();

    /**
     * 健康监控配置
     */
    private HealthProperties health = new HealthProperties();

    /**
     * 动态数据源配置
     */
    private DynamicDataSourceProperties dynamicDatasource = new DynamicDataSourceProperties();

    /**
     * 故障转移配置
     */
    private FailoverProperties failover = new FailoverProperties();

    /**
     * SQL统计配置
     */
    private SqlStatisticsProperties sqlStatistics = new SqlStatisticsProperties();

    /**
     * 迁移配置
     */
    private MigrationProperties migration = new MigrationProperties();

    /**
     * 审计系统配置
     */
    @Data
    public static class AuditProperties {
        /**
         * 是否启用审计
         */
        private boolean enabled = true;

        /**
         * 是否异步处理
         */
        private boolean async = true;

        /**
         * 线程池配置
         */
        private ThreadPoolProperties threadPool = new ThreadPoolProperties();

        /**
         * 日志保留天数
         */
        private int retentionDays = 90;

        /**
         * 排除的表（不记录审计日志）
         */
        private List<String> excludedTables = new ArrayList<>();

        /**
         * 归档配置
         */
        private ArchiveProperties archive = new ArchiveProperties();
    }

    /**
     * 归档配置
     */
    @Data
    public static class ArchiveProperties {
        /**
         * 是否启用归档（如果禁用，则直接删除过期日志）
         */
        private boolean enabled = true;

        /**
         * 归档数据保留天数（归档表中的数据保留时间）
         */
        private int archiveRetentionDays = 365;

        /**
         * 自动清理定时任务的cron表达式
         * 默认每天凌晨2点执行
         */
        private String cleanupCron = "0 0 2 * * ?";

        /**
         * 是否启用自动清理定时任务
         */
        private boolean autoCleanupEnabled = true;
    }

    /**
     * 多租户配置
     */
    @Data
    public static class MultiTenancyProperties {
        /**
         * 是否启用多租户
         */
        private boolean enabled = false;

        /**
         * 隔离模式: SHARED_DB, SEPARATE_DB, SEPARATE_SCHEMA
         */
        private String isolationMode = "SHARED_DB";

        /**
         * 租户字段名
         */
        private String tenantColumn = "tenant_id";

        /**
         * 排除的表（不添加租户过滤）
         */
        private List<String> excludedTables = new ArrayList<>();
    }

    /**
     * 数据安全配置
     */
    @Data
    public static class SecurityProperties {
        /**
         * 加密配置
         */
        private EncryptionProperties encryption = new EncryptionProperties();

        /**
         * 脱敏配置
         */
        private MaskingProperties masking = new MaskingProperties();
    }

    /**
     * 加密配置
     */
    @Data
    public static class EncryptionProperties {
        /**
         * 是否启用加密
         */
        private boolean enabled = false;

        /**
         * 加密算法: AES, SM4
         */
        private String algorithm = "AES";

        /**
         * 密钥
         */
        private String secretKey;
    }

    /**
     * 脱敏配置
     */
    @Data
    public static class MaskingProperties {
        /**
         * 是否启用脱敏
         */
        private boolean enabled = false;

        /**
         * 脱敏规则
         */
        private Map<String, String> rules = new HashMap<>();
    }

    /**
     * 健康监控配置
     */
    @Data
    public static class HealthProperties {
        /**
         * 是否启用健康监控
         */
        private boolean enabled = true;

        /**
         * 检查间隔（秒）
         */
        private int checkInterval = 30;

        /**
         * 慢查询阈值（毫秒）
         */
        private long slowQueryThreshold = 1000;

        /**
         * 连接池告警阈值（百分比）
         */
        private int poolUsageThreshold = 80;
    }

    /**
     * 动态数据源配置
     */
    @Data
    public static class DynamicDataSourceProperties {
        /**
         * 是否启用动态数据源
         */
        private boolean enabled = false;

        /**
         * 默认数据源
         */
        private String primary = "master";

        /**
         * 严格模式（数据源不存在时抛异常）
         */
        private boolean strict = true;
    }

    /**
     * 故障转移配置
     */
    @Data
    public static class FailoverProperties {
        /**
         * 是否启用故障转移
         */
        private boolean enabled = true;

        /**
         * 重连尝试次数
         */
        private int maxRetry = 3;

        /**
         * 重连间隔（毫秒）
         */
        private long retryInterval = 5000;

        /**
         * 主库降级（主库不可用时是否降级到只读）
         */
        private boolean masterDegradation = false;
    }

    /**
     * SQL统计配置
     */
    @Data
    public static class SqlStatisticsProperties {
        /**
         * 是否启用SQL统计
         */
        private boolean enabled = false;

        /**
         * 统计数据保留天数
         */
        private int retentionDays = 30;

        /**
         * 是否启用执行计划分析
         */
        private boolean explainEnabled = false;
    }

    /**
     * 线程池配置
     */
    @Data
    public static class ThreadPoolProperties {
        /**
         * 核心线程数
         */
        private int coreSize = 2;

        /**
         * 最大线程数
         */
        private int maxSize = 5;

        /**
         * 队列容量
         */
        private int queueCapacity = 1000;
    }

    /**
     * 迁移配置
     */
    @Data
    public static class MigrationProperties {
        /**
         * 备份目录
         */
        private String backupDir = "./backups";

        /**
         * 是否在迁移前自动创建备份
         */
        private boolean autoBackup = true;

        /**
         * 生产环境是否需要确认
         */
        private boolean requireConfirmation = true;

        /**
         * 确认令牌有效期（分钟）
         */
        private int tokenValidityMinutes = 30;

        /**
         * 迁移失败时是否自动回滚
         */
        private boolean autoRollback = true;
    }
}
