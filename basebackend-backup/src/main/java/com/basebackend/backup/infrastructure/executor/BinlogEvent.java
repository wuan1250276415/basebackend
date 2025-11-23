package com.basebackend.backup.infrastructure.executor;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Binlog事件对象
 * 封装从binlog中解析出的数据变更事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BinlogEvent {

    /**
     * 事件类型
     */
    private EventType eventType;

    /**
     * 数据库名
     */
    private String database;

    /**
     * 表名
     */
    private String table;

    /**
     * 操作类型：INSERT、UPDATE、DELETE
     */
    private OperationType operation;

    /**
     * 变更前的数据（用于UPDATE/DELETE）
     */
    private List<ColumnValue> beforeColumns;

    /**
     * 变更后的数据（用于INSERT/UPDATE）
     */
    private List<ColumnValue> afterColumns;

    /**
     * 事件时间
     */
    private LocalDateTime timestamp;

    /**
     * Binlog文件名
     */
    private String binlogFilename;

    /**
     * 事件位置
     */
    private long position;

    /**
     * 事件大小
     */
    private long eventSize;

    /**
     * 事件序号
     */
    private long eventNumber;

    /**
     * 事件类型枚举
     */
    public enum EventType {
        WRITE_ROWS,    // 插入行
        UPDATE_ROWS,   // 更新行
        DELETE_ROWS,   // 删除行
        QUERY,         // 查询语句
        UNKNOWN        // 未知类型
    }

    /**
     * 操作类型枚举
     */
    public enum OperationType {
        INSERT, UPDATE, DELETE, UNKNOWN
    }

    /**
     * 列值对象
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnValue {
        private String columnName;
        private Object value;
        private String dataType;
        private boolean isKey;
    }

    /**
     * 判断是否为INSERT操作
     */
    public boolean isInsert() {
        return operation == OperationType.INSERT;
    }

    /**
     * 判断是否为UPDATE操作
     */
    public boolean isUpdate() {
        return operation == OperationType.UPDATE;
    }

    /**
     * 判断是否为DELETE操作
     */
    public boolean isDelete() {
        return operation == OperationType.DELETE;
    }

    /**
     * 获取主键值
     */
    public Object getPrimaryKeyValue() {
        if (beforeColumns != null) {
            return beforeColumns.stream()
                .filter(ColumnValue::isKey)
                .map(ColumnValue::getValue)
                .findFirst()
                .orElse(null);
        }
        if (afterColumns != null) {
            return afterColumns.stream()
                .filter(ColumnValue::isKey)
                .map(ColumnValue::getValue)
                .findFirst()
                .orElse(null);
        }
        return null;
    }
}
