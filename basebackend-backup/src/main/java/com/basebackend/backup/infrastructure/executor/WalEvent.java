package com.basebackend.backup.infrastructure.executor;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PostgreSQL WAL事件对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalEvent {

    /**
     * 操作类型：INSERT、UPDATE、DELETE、DDL
     */
    private String operation;

    /**
     * 数据库名
     */
    private String database;

    /**
     * 表名
     */
    private String table;

    /**
     * 事件时间
     */
    private LocalDateTime timestamp;

    /**
     * WAL文件偏移量
     */
    private long offset;

    /**
     * 记录长度
     */
    private int length;

    /**
     * 记录序号
     */
    private long recordNumber;

    /**
     * XID（事务ID）
     */
    private long xid;

    /**
     * 变更的数据（JSON格式）
     */
    private String data;

    /**
     * 判断是否为INSERT操作
     */
    public boolean isInsert() {
        return "INSERT".equals(operation);
    }

    /**
     * 判断是否为UPDATE操作
     */
    public boolean isUpdate() {
        return "UPDATE".equals(operation);
    }

    /**
     * 判断是否为DELETE操作
     */
    public boolean isDelete() {
        return "DELETE".equals(operation);
    }

    /**
     * 判断是否为DDL操作
     */
    public boolean isDDL() {
        return "DDL".equals(operation);
    }
}
