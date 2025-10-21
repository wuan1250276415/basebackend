package com.basebackend.backup.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 备份配置属性
 *
 * @author BaseBackend
 */
@Data
@Component
@ConfigurationProperties(prefix = "backup")
public class BackupProperties {

    /**
     * 是否启用备份
     */
    private boolean enabled = true;

    /**
     * 备份存储路径
     */
    private String backupPath = "/data/backup/mysql";

    /**
     * 备份文件保留天数
     */
    private int retentionDays = 30;

    /**
     * 是否启用自动备份
     */
    private boolean autoBackupEnabled = true;

    /**
     * 自动备份CRON表达式（默认每天凌晨2点）
     */
    private String autoBackupCron = "0 0 2 * * ?";

    /**
     * 是否启用增量备份
     */
    private boolean incrementalBackupEnabled = true;

    /**
     * Binlog路径
     */
    private String binlogPath = "/var/lib/mysql";

    /**
     * mysqldump命令路径
     */
    private String mysqldumpPath = "mysqldump";

    /**
     * mysql命令路径
     */
    private String mysqlPath = "mysql";

    /**
     * mysqlbinlog命令路径
     */
    private String mysqlbinlogPath = "mysqlbinlog";

    /**
     * 数据库配置
     */
    private DatabaseConfig database = new DatabaseConfig();

    @Data
    public static class DatabaseConfig {
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
        private String password;

        /**
         * 要备份的数据库名称
         */
        private String database = "basebackend";
    }
}
