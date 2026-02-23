package com.basebackend.logging.pipeline;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 日志管道配置属性
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Data
@ConfigurationProperties(prefix = "basebackend.logging.pipeline")
public class LogPipelineProperties {

    /**
     * 是否启用日志管道
     */
    private boolean enabled = false;

    /**
     * 传输类型 (console, file)
     */
    private String transport = "console";

    /**
     * WAL 配置
     */
    private WalConfig wal = new WalConfig();

    /**
     * 批量发送大小
     */
    private int batchSize = 100;

    /**
     * 批量发送超时
     */
    private Duration batchTimeout = Duration.ofSeconds(5);

    @Data
    public static class WalConfig {
        /**
         * 是否启用本地 WAL
         */
        private boolean enabled = false;

        /**
         * WAL 文件目录
         */
        private String directory = "logs/wal";

        /**
         * 单个 WAL 文件最大大小（字节），默认 32MB
         */
        private long maxFileSizeBytes = 33554432L;

        /**
         * WAL 文件保留数量
         */
        private int maxFiles = 10;
    }
}
