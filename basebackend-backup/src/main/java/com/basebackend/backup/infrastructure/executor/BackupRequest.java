package com.basebackend.backup.infrastructure.executor;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 备份请求对象
 * 包含执行备份所需的全部信息
 */
@Data
public class BackupRequest {
    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 数据源类型
     */
    private String datasourceType;

    /**
     * 备份类型
     */
    private String backupType;

    /**
     * 策略配置（JSON格式）
     */
    private String strategyJson;

    /**
     * 存储策略配置（JSON格式）
     */
    private String storagePolicyJson;

    /**
     * 数据库连接信息
     */
    private DatabaseConfig databaseConfig;

    /**
     * 备份开始时间
     */
    private LocalDateTime startTime;

    /**
     * 增量备份起始位置（binlog position或WAL position）
     */
    private String startPosition;

    /**
     * 自定义参数
     */
    private Map<String, Object> parameters;

    @Data
    public static class DatabaseConfig {
        /**
         * 数据库主机
         */
        private String host;

        /**
         * 端口
         */
        private Integer port;

        /**
         * 用户名
         */
        private String username;

        /**
         * 密码
         */
        private String password;

        /**
         * 数据库名称
         */
        private String database;

        /**
         * 字符集
         */
        private String charset = "utf8mb4";
    }
}
