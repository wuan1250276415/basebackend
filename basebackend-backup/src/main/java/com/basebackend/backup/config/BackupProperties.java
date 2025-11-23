package com.basebackend.backup.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 备份系统配置属性
 * 支持企业级备份功能：分布式锁、重试机制、校验、多副本存储、增量备份等
 */
@Data
@Component
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
     * 是否启用增量备份
     */
    private boolean incrementalBackupEnabled = true;

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
            private boolean enabled = true;
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
            private boolean enabled = true;
            private List<Replica> replicas = new ArrayList<>();

            public MultiReplica() {
                replicas.add(new Replica("local", 1, true));
                replicas.add(new Replica("s3", 2, true));
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
    }
}
