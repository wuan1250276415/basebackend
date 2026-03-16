package com.basebackend.backup.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 备份系统配置属性
 * 支持企业级备份功能：分布式锁、重试机制、校验、多副本存储、增量备份等
 */
@Data
@ConfigurationProperties(prefix = "backup")
public class BackupProperties {

    /**
     * 是否启用备份系统
     */
    private boolean enabled = true;

    /**
     * 分布式锁配置
     */
    private DistributedLock distributedLock = new DistributedLock();

    /**
     * 重试配置
     */
    private Retry retry = new Retry();

    /**
     * 校验配置
     */
    private Checksum checksum = new Checksum();

    /**
     * 存储配置
     */
    private Storage storage = new Storage();

    /**
     * 增量备份配置
     */
    private Incremental incremental = new Incremental();

    /**
     * 监控配置
     */
    private Metrics metrics = new Metrics();

    /**
     * 通知配置
     */
    private Notify notify = new Notify();

    /**
     * 加密配置
     */
    private Encryption encryption = new Encryption();

    /**
     * PostgreSQL数据库配置
     */
    private PostgresConfig postgres = new PostgresConfig();

    /**
     * MySQL数据库配置
     */
    private DatabaseConfig database = new DatabaseConfig();

    /**
     * 备份路径
     */
    private String backupPath = "/data/backups/mysql";

    /**
     * 备份保留天数
     */
    private int retentionDays = 30;

    /**
     * mysqldump可执行文件路径
     */
    private String mysqldumpPath = "mysqldump";

    /**
     * mysql可执行文件路径
     */
    private String mysqlPath = "mysql";

    /**
     * mysqlbinlog可执行文件路径
     */
    private String mysqlbinlogPath = "mysqlbinlog";

    /**
     * 是否启用增量备份
     */
    private boolean incrementalBackupEnabled = false;

    @Data
    public static class DistributedLock {
        /**
         * 锁类型：redisson
         */
        private String type = "redisson";

        /**
         * 锁键前缀
         */
        private String keyPrefix = "backup:lock:";

        /**
         * 锁TTL（默认5分钟）
         */
        private Duration ttl = Duration.ofMinutes(5);

        /**
         * 获取锁等待时间（默认10秒）
         */
        private Duration waitTime = Duration.ofSeconds(10);
    }

    @Data
    public static class Retry {
        /**
         * 最大重试次数
         */
        private int maxAttempts = 5;

        /**
         * 退避策略
         */
        private Backoff backoff = new Backoff();

        @Data
        public static class Backoff {
            /**
             * 初始延迟时间（默认2秒）
             */
            private Duration initial = Duration.ofSeconds(2);

            /**
             * 退避倍数（默认2.0）
             */
            private double multiplier = 2.0;

            /**
             * 最大延迟时间（默认1分钟）
             */
            private Duration max = Duration.ofMinutes(1);
        }
    }

    @Data
    public static class Checksum {
        /**
         * 启用校验
         */
        private boolean enabled = true;

        /**
         * 校验算法：MD5, SHA256
         */
        private List<String> algorithms = List.of("MD5", "SHA256");
    }

    @Data
    public static class Storage {
        /**
         * 本地存储配置
         */
        private Local local = new Local();

        /**
         * S3存储配置
         */
        private S3 s3 = new S3();

        /**
         * OSS存储配置
         */
        private Oss oss = new Oss();

        /**
         * 多副本策略
         */
        private MultiReplica multiReplica = new MultiReplica();

        @Data
        public static class Local {
            private boolean enabled = true;
            private String basePath = "/data/backups";
            private int retentionDays = 7;
        }

        @Data
        public static class S3 {
            private boolean enabled = false;
            private String endpoint;
            private String bucket = "basebackend-backup";
            private String accessKey;
            private String secretKey;
            private String region = "us-east-1";
            private int retentionDays = 30;
            private Long multipartChunkSize = 16L * 1024 * 1024; // 16MB
            private Duration connectionTimeout = Duration.ofSeconds(60);
            private Duration readTimeout = Duration.ofSeconds(300);
        }

        @Data
        public static class Oss {
            private boolean enabled = false;
            private String endpoint;
            private String bucket = "basebackend-backup";
            private String accessKey;
            private String secretKey;
            private int retentionDays = 30;
        }

        @Data
        public static class MultiReplica {
            private boolean enabled = false;
            private List<Replica> replicas = new ArrayList<>();

            public MultiReplica() {
                replicas.add(new Replica("local", 1, true));
                replicas.add(new Replica("s3", 2, false));
            }

            @Data
            public static class Replica {
                private String type;
                private int priority;
                private boolean enabled;

                public Replica() {}

                public Replica(String type, int priority, boolean enabled) {
                    this.type = type;
                    this.priority = priority;
                    this.enabled = enabled;
                }
            }
        }
    }

    @Data
    public static class Incremental {
        /**
         * MySQL增量备份配置
         */
        private Mysql mysql = new Mysql();

        /**
         * PostgreSQL增量备份配置
         */
        private Postgres postgres = new Postgres();

        @Data
        public static class Mysql {
            private boolean enabled = true;
            private String binlogDir = "/var/lib/mysql";
            private Duration maxRetention = Duration.ofDays(7);
        }

        @Data
        public static class Postgres {
            private boolean enabled = true;
            private String walDir = "/var/lib/postgresql/data/pg_wal";
            private Duration maxRetention = Duration.ofDays(7);
        }
    }

    @Data
    public static class Metrics {
        /**
         * 启用监控指标
         */
        private boolean enabled = true;

        /**
         * 指标前缀
         */
        private String prefix = "backup";
    }

    @Data
    public static class Notify {
        /**
         * 邮件通知配置
         */
        private Email email = new Email();

        /**
         * Slack通知配置
         */
        private Slack slack = new Slack();

        /**
         * 钉钉通知配置
         */
        private DingTalk dingTalk = new DingTalk();

        @Data
        public static class Email {
            private boolean enabled = false;
            private String smtpHost;
            private int smtpPort = 587;
            private String username;
            private String password;
            private List<String> recipients = new ArrayList<>();
        }

        @Data
        public static class Slack {
            private boolean enabled = false;
            private String webhookUrl;
            private String channel = "#backup-alerts";
        }

        @Data
        public static class DingTalk {
            private boolean enabled = false;
            private String webhookUrl;
            private String secret;
        }
    }

    /**
     * 数据库配置
     */
    @Data
    public static class DatabaseConfig {
        /**
         * 数据库名
         */
        private String database = "basebackend";

        /**
         * 数据库主机
         */
        private String host = "localhost";

        /**
         * 数据库端口
         */
        private int port = 3306;

        /**
         * 数据库用户名
         */
        private String username = "root";

        /**
         * 数据库密码
         */
        private String password = "";

        /**
         * 是否启用SSL连接（生产环境建议设为true）
         */
        private boolean sslEnabled = true;
    }

    /**
     * 加密配置
     */
    @Data
    public static class Encryption {
        /**
         * 是否启用加密
         */
        private boolean enabled = false;

        /**
         * 加密算法
         */
        private String algorithm = "AES/GCM/NoPadding";

        /**
         * 加密密钥
         */
        private String key;

        /**
         * 本地加密密钥（Base64编码）
         */
        private String localKey;

        /**
         * IV长度（字节）
         */
        private int ivLengthBytes = 12;

        /**
         * GCM Tag长度（位）
         */
        private int tagLengthBits = 128;
    }

    /**
     * PostgreSQL数据库配置
     */
    @Data
    public static class PostgresConfig {
        /**
         * 数据库名
         */
        private String database = "basebackend";

        /**
         * 数据库主机
         */
        private String host = "localhost";

        /**
         * 数据库端口
         */
        private int port = 5432;

        /**
         * 数据库用户名
         */
        private String username = "postgres";

        /**
         * 数据库密码
         */
        private String password = "";

        /**
         * 备份路径
         */
        private String backupPath = "/data/backups/postgresql";

        /**
         * 备份保留天数
         */
        private int retentionDays = 30;

        /**
         * pg_dump可执行文件路径
         */
        private String pgDumpPath = "pg_dump";

        /**
         * psql可执行文件路径
         */
        private String psqlPath = "psql";

        /**
         * pg_basebackup可执行文件路径
         */
        private String pgBasebackupPath = "pg_basebackup";

        /**
         * pg_waldump可执行文件路径
         */
        private String pgWalDumpPath = "pg_waldump";

        /**
         * pg_ctl可执行文件路径（用于内建物理回放编排）
         */
        private String pgCtlPath = "pg_ctl";

        /**
         * 是否启用PostgreSQL增量回放能力
         * <p>
         * 当前默认关闭：避免在回放链路未完整实现前继续产出不可恢复的增量备份。
         */
        private boolean incrementalReplayEnabled = false;

        /**
         * PostgreSQL增量回放模式：
         * logical_snapshot - 逻辑快照回放（默认）
         * wal_external - WAL导出 + 外部命令回放
         * wal_physical_builtin - WAL导出 + 内建物理回放编排
         */
        private String incrementalReplayMode = "logical_snapshot";

        /**
         * WAL回放外部命令模板（仅在 wal_external 模式下生效）
         * <p>
         * 支持占位符：
         * ${artifact} ${targetDatabase} ${walStart} ${walEnd}
         * ${host} ${port} ${username} ${password}
         */
        private String walReplayCommand = "";

        /**
         * 内建物理回放数据目录（PostgreSQL -D 目录）
         */
        private String physicalReplayDataDir = "/var/lib/postgresql/data";

        /**
         * 内建物理回放 WAL 归档目录（restore_command 从此目录读取 WAL 文件）
         */
        private String physicalReplayArchiveDir = "/var/lib/postgresql/wal_archive";

        /**
         * 内建物理回放基线目录（pg_basebackup 输出目录根路径）
         */
        private String physicalReplayBaselineDir = "/var/lib/postgresql/physical_baselines";

        /**
         * 内建物理回放 restore_command 模板
         * <p>
         * 默认会展开 ${archiveDir} 占位符，且必须包含 %f 与 %p
         */
        private String physicalReplayRestoreCommandTemplate = "cp ${archiveDir}/%f \"%p\"";

        /**
         * 内建物理回放 pg_basebackup 超时（秒）
         */
        private int physicalReplayBasebackupTimeoutSeconds = 300;

        /**
         * 内建物理回放 pg_basebackup checkpoint 模式（fast/spread）
         */
        private boolean physicalReplayBasebackupFastCheckpoint = true;

        /**
         * 内建物理回放：pg_basebackup 失败时是否自动清理残留基线目录
         */
        private boolean physicalReplayBaselineCleanupOnBasebackupFailure = true;

        /**
         * 内建物理回放停止超时（秒）
         */
        private int physicalReplayStopTimeoutSeconds = 60;

        /**
         * 内建物理回放启动超时（秒）
         */
        private int physicalReplayStartTimeoutSeconds = 120;

        /**
         * 内建物理回放成功后保留的基线数量
         */
        private int physicalReplayKeepLatestBaselines = 3;

        /**
         * 回放失败时是否自动执行回滚启动
         */
        private boolean physicalReplayRollbackOnFailure = true;

        /**
         * 回放失败自动回滚启动超时（秒）
         */
        private int physicalReplayRollbackStartTimeoutSeconds = 120;

        /**
         * 回放失败自动回滚后是否执行健康探针（进程 + SQL）
         */
        private boolean physicalReplayRollbackHealthProbeEnabled = true;

        /**
         * 回放失败自动回滚健康探针最大尝试次数
         */
        private int physicalReplayRollbackHealthProbeMaxAttempts = 1;

        /**
         * 回放失败自动回滚健康探针重试间隔（秒）
         */
        private int physicalReplayRollbackHealthProbeIntervalSeconds = 1;

        /**
         * 回放失败自动回滚健康探针超时（秒）
         */
        private int physicalReplayRollbackHealthProbeTimeoutSeconds = 1;

        /**
         * 回放失败自动回滚后是否执行业务一致性探针
         */
        private boolean physicalReplayRollbackBusinessProbeEnabled = false;

        /**
         * 回放失败自动回滚业务一致性探针SQL（需返回至少1行1列）
         */
        private String physicalReplayRollbackBusinessProbeSql = "";

        /**
         * 回放失败自动回滚业务一致性探针期望值（首列比对，留空表示仅要求非空）
         */
        private String physicalReplayRollbackBusinessProbeExpectedValue = "";

        /**
         * 内建物理回放 recovery_target_action（pause/promote/shutdown）
         */
        private String physicalReplayRecoveryTargetAction = "promote";

        /**
         * 内建物理回放启动端口覆盖，<=0 表示不覆盖默认端口
         */
        private int physicalReplayPort = -1;
    }
}
