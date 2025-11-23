package com.basebackend.backup.infrastructure.executor;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 备份产物对象
 * 包含备份执行后生成的所有信息和文件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupArtifact {

    /**
     * 备份文件
     */
    private File file;

    /**
     * 备份类型：full/incremental
     */
    private String backupType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 备份开始时间
     */
    private LocalDateTime startTime;

    /**
     * 备份结束时间
     */
    private LocalDateTime endTime;

    /**
     * 备份耗时（秒）
     */
    private Long durationSeconds;

    /**
     * MySQL binlog起始位置
     */
    private String binlogStartPosition;

    /**
     * MySQL binlog结束位置
     */
    private String binlogEndPosition;

    /**
     * PostgreSQL WAL起始位置
     */
    private String walStartPosition;

    /**
     * PostgreSQL WAL结束位置
     */
    private String walEndPosition;

    /**
     * 包含的表列表
     */
    private List<String> includedTables;

    /**
     * 排除的表列表
     */
    private List<String> excludedTables;

    /**
     * 压缩前文件大小
     */
    private Long uncompressedSize;

    /**
     * 压缩算法
     */
    private String compressionAlgorithm;

    /**
     * 压缩比
     */
    private Double compressionRatio;

    /**
     * 增量链ID（用于关联全量备份）
     */
    private String incrementalChainId;

    /**
     * 元数据信息
     */
    private Metadata metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        /**
         * 备份命令
         */
        private String backupCommand;

        /**
         * 数据源版本
         */
        private String datasourceVersion;

        /**
         * 备份工具版本
         */
        private String backupToolVersion;

        /**
         * 备份时数据库大小
         */
        private Long databaseSize;

        /**
         * 备份的记录数
         */
        private Long recordCount;

        /**
         * 自定义标签
         */
        private List<String> tags;
    }
}
