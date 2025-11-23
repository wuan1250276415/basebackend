package com.basebackend.backup.infrastructure.executor;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 增量备份请求对象
 * 继承自BackupRequest，添加增量备份特有字段
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IncrementalBackupRequest extends BackupRequest {

    /**
     * 基线全量备份ID
     */
    private Long baseFullBackupId;

    /**
     * 增量起始binlog位置
     */
    private String binlogStartPosition;

    /**
     * 增量起始binlog文件名
     */
    private String binlogStartFile;

    /**
     * 增量结束binlog位置
     */
    private String binlogEndPosition;

    /**
     * 增量结束binlog文件名
     */
    private String binlogEndFile;
}
