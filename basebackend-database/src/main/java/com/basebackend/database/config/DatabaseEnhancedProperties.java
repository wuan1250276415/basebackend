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
     * SQL注入防护配置
     */
    private SqlInjectionProperties sqlInjection = new SqlInjectionProperties();
    
    /**
     * Prometheus 监控指标配置
     */
    private MetricsProperties metrics = new MetricsProperties();

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
         * 加密算法: AES, SM4, RSA
         */
        private String algorithm = "AES";

        /**
         * 密钥
         */
        private String secretKey;
        
        /**
         * 严格模式（加密失败时是否抛出异常）
         * true: 加密失败时抛出异常，阻止业务操作
         * false: 加密失败时记录告警，允许业务操作继续（可能以明文存储）
         */
        private boolean strictMode = true;
        
        /**
         * 密钥来源: CONFIG, ENV, VAULT
         * CONFIG: 从配置文件读取（默认，不推荐生产环境使用）
         * ENV: 从环境变量读取
         * VAULT: 从外部密钥管理系统读取（如 HashiCorp Vault）
         */
        private String keySource = "CONFIG";
        
        /**
         * 环境变量名称（当 keySource=ENV 时使用）
         */
        private String keyEnvVariable = "DATABASE_ENCRYPTION_KEY";
        
        /**
         * Vault 配置（当 keySource=VAULT 时使用）
         */
        private VaultProperties vault = new VaultProperties();
        
        /**
         * 密钥轮换配置
         */
        private KeyRotationProperties keyRotation = new KeyRotationProperties();
    }
    
    /**
     * Vault 密钥管理配置
     */
    @Data
    public static class VaultProperties {
        /**
         * Vault 服务地址
         */
        private String address = "http://localhost:8200";
        
        /**
         * Vault 认证令牌
         */
        private String token;
        
        /**
         * 密钥路径
         */
        private String secretPath = "secret/data/database/encryption";
        
        /**
         * 密钥字段名
         */
        private String keyField = "key";
    }
    
    /**
     * 密钥轮换配置
     */
    @Data
    public static class KeyRotationProperties {
        /**
         * 是否启用密钥轮换
         */
        private boolean enabled = false;
        
        /**
         * 旧密钥（用于解密旧数据）
         */
        private String previousKey;
        
        /**
         * 轮换时间戳（在此时间之前的数据使用旧密钥）
         */
        private String rotationTimestamp;
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
     * SQL注入防护配置
     */
    @Data
    public static class SqlInjectionProperties {
        /**
         * 是否启用SQL注入防护
         */
        private boolean enabled = true;
        
        /**
         * 白名单SQL模式（正则表达式列表，匹配的SQL跳过检测）
         */
        private List<String> whitelistPatterns = new ArrayList<>();
        
        /**
         * 白名单Mapper方法（完整方法ID列表，如 com.example.mapper.UserMapper.customQuery）
         */
        private List<String> whitelistMappers = new ArrayList<>();
        
        /**
         * 是否记录被阻止的SQL（用于调试）
         */
        private boolean logBlockedSql = true;
        
        /**
         * 是否启用严格模式（检测到注入时抛出异常还是仅记录警告）
         */
        private boolean strictMode = true;
    }
    
    /**
     * Prometheus 监控指标配置
     */
    @Data
    public static class MetricsProperties {
        /**
         * 是否启用 Prometheus 指标导出
         */
        private boolean enabled = true;
        
        /**
         * 指标前缀
         */
        private String prefix = "database_enhanced";
        
        /**
         * 是否导出连接池指标
         */
        private boolean connectionPoolMetrics = true;
        
        /**
         * 是否导出审计指标
         */
        private boolean auditMetrics = true;
        
        /**
         * 是否导出加密指标
         */
        private boolean encryptionMetrics = true;
        
        /**
         * 是否导出SQL统计指标
         */
        private boolean sqlStatisticsMetrics = true;
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
