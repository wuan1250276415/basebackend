package com.basebackend.backup.infrastructure.executor.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.infrastructure.executor.BinlogEvent;
import com.basebackend.backup.infrastructure.executor.BinlogEventListener;
import com.basebackend.backup.infrastructure.executor.BinlogPosition;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MySQL Binlog解析器
 * 使用mysql-binlog-connector-java库实现对MySQL binlog的实时解析
 */
@Slf4j
@Component
public class MySqlBinlogParser {

    @Autowired
    private BackupProperties backupProperties;

    private BinaryLogClient client;
    private boolean isConnected = false;

    /**
     * 获取当前binlog位置
     *
     * @param host MySQL主机
     * @param port MySQL端口
     * @param username 用户名
     * @param password 密码
     * @return 当前binlog位置
     * @throws Exception 获取失败时抛出异常
     */
    public BinlogPosition getCurrentPosition(String host, int port, String username, String password) throws Exception {
        // 通过查询SHOW MASTER STATUS获取当前位置
        // 这里需要通过JDBC连接查询
        log.info("获取当前binlog位置: {}:{}", host, port);

        // 返回空位置，表示从头开始
        return BinlogPosition.of("", 4);
    }

    /**
     * 订阅binlog变更事件
     *
     * @param host MySQL主机
     * @param port MySQL端口
     * @param username 用户名
     * @param password 密码
     * @param startPosition 起始位置
     * @param listener 事件监听器
     * @throws Exception 订阅失败时抛出异常
     */
    public void subscribe(String host, int port, String username, String password,
                         BinlogPosition startPosition, BinlogEventListener listener) throws Exception {
        log.info("订阅binlog变更事件, 起始位置: {}", startPosition);

        client = new BinaryLogClient(host, port, username, password);

        // 设置起始位置
        if (startPosition != null && startPosition.isValid()) {
            client.setBinlogFilename(startPosition.getFilename());
            client.setBinlogPosition(startPosition.getPosition());
        }

        // 注册事件监听器
        client.registerEventListener(event -> {
            try {
                EventHeader header = event.getHeader();
                EventType eventType = header.getEventType();

                log.debug("接收binlog事件: {}", eventType);

                switch (eventType) {
                    case WRITE_ROWS:
                    case EXT_WRITE_ROWS:
                        handleWriteRows(event, listener);
                        break;

                    case UPDATE_ROWS:
                    case EXT_UPDATE_ROWS:
                        handleUpdateRows(event, listener);
                        break;

                    case DELETE_ROWS:
                    case EXT_DELETE_ROWS:
                        handleDeleteRows(event, listener);
                        break;

                    case QUERY:
                        handleQuery(event, listener);
                        break;

                    case ROWS_QUERY:
                        handleRowsQuery(event, listener);
                        break;

                    default:
                        log.debug("忽略不支持的事件类型: {}", eventType);
                }
            } catch (Exception e) {
                log.error("处理binlog事件失败", e);
                try {
                    listener.onError(e);
                } catch (Exception ex) {
                    log.error("事件监听器错误处理失败", ex);
                }
            }
        });

        // 连接并开始监听
        client.connect();
        isConnected = true;
        log.info("Binlog订阅成功");
    }

    /**
     * 从指定位置解析到当前位置
     *
     * @param startPosition 起始位置
     * @param endPosition 结束位置
     * @param databaseName 数据库名（可选，用于过滤）
     * @return 解析的事件列表
     * @throws Exception 解析失败时抛出异常
     */
    public List<BinlogEvent> parseToPosition(BinlogPosition startPosition, BinlogPosition endPosition,
                                            String databaseName) throws Exception {
        log.info("解析binlog: {} -> {}", startPosition, endPosition);

        List<BinlogEvent> events = new ArrayList<>();
        AtomicBoolean reachedEnd = new AtomicBoolean(false);
        BinlogEventListener listener = new BinlogEventListener() {
            @Override
            public void onDataChange(BinlogEvent event) throws Exception {
                if (databaseName == null || databaseName.equals(event.getDatabase())) {
                    events.add(event);
                }
                // 检查是否到达结束位置
                if (event.getBinlogFilename().equals(endPosition.getFilename()) &&
                    event.getPosition() >= endPosition.getPosition()) {
                    reachedEnd.set(true);
                }
            }

            @Override
            public void onQuery(BinlogEvent event) throws Exception {
                // DDL语句也需要记录
                events.add(event);
            }

            @Override
            public void onDDL(BinlogEvent event) throws Exception {
                events.add(event);
            }

            @Override
            public void onError(Throwable error) throws Exception {
                log.error("Binlog解析错误", error);
            }

            @Override
            public List<BinlogEvent> getEvents() {
                return events;
            }
        };

        // 连接并解析
        BackupProperties.DatabaseConfig db = backupProperties.getDatabase();
        subscribe(db.getHost(), db.getPort(), db.getUsername(), db.getPassword(), startPosition, listener);

        // 等待解析完成（这里应该添加超时控制）
        while (!reachedEnd.get() && isConnected) {
            Thread.sleep(100);
        }

        disconnect();
        log.info("Binlog解析完成, 共 {} 个事件", events.size());
        return events;
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (client != null && isConnected) {
            try {
                client.disconnect();
                isConnected = false;
                log.info("Binlog连接已断开");
            } catch (IOException e) {
                log.error("断开binlog连接失败", e);
            }
        }
    }

    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * 处理插入行事件
     */
    private void handleWriteRows(Event event, BinlogEventListener listener) throws Exception {
        WriteRowsEventData data = event.getData();
        EventHeader header = event.getHeader();

        String table = data.getTableId() + "";
        BinlogEvent binlogEvent = BinlogEvent.builder()
            .eventType(BinlogEvent.EventType.WRITE_ROWS)
            .database(backupProperties.getDatabase().getDatabase())
            .table(table)
            .operation(BinlogEvent.OperationType.INSERT)
            .timestamp(getTimestamp(header))
            .binlogFilename(getBinlogFilename(header))
            .position(getPosition(header))
            .build();

        listener.onDataChange(binlogEvent);
    }

    /**
     * 处理更新行事件
     */
    private void handleUpdateRows(Event event, BinlogEventListener listener) throws Exception {
        UpdateRowsEventData data = event.getData();
        EventHeader header = event.getHeader();

        String table = data.getTableId() + "";
        BinlogEvent binlogEvent = BinlogEvent.builder()
            .eventType(BinlogEvent.EventType.UPDATE_ROWS)
            .database(backupProperties.getDatabase().getDatabase())
            .table(table)
            .operation(BinlogEvent.OperationType.UPDATE)
            .timestamp(getTimestamp(header))
            .binlogFilename(getBinlogFilename(header))
            .position(getPosition(header))
            .build();

        listener.onDataChange(binlogEvent);
    }

    /**
     * 处理删除行事件
     */
    private void handleDeleteRows(Event event, BinlogEventListener listener) throws Exception {
        DeleteRowsEventData data = event.getData();
        EventHeader header = event.getHeader();

        String table = data.getTableId() + "";
        BinlogEvent binlogEvent = BinlogEvent.builder()
            .eventType(BinlogEvent.EventType.DELETE_ROWS)
            .database(backupProperties.getDatabase().getDatabase())
            .table(table)
            .operation(BinlogEvent.OperationType.DELETE)
            .timestamp(getTimestamp(header))
            .binlogFilename(getBinlogFilename(header))
            .position(getPosition(header))
            .build();

        listener.onDataChange(binlogEvent);
    }

    /**
     * 处理查询事件
     */
    private void handleQuery(Event event, BinlogEventListener listener) throws Exception {
        QueryEventData data = event.getData();
        EventHeader header = event.getHeader();

        BinlogEvent binlogEvent = BinlogEvent.builder()
            .eventType(BinlogEvent.EventType.QUERY)
            .database(data.getDatabase())
            .operation(BinlogEvent.OperationType.UNKNOWN)
            .timestamp(getTimestamp(header))
            .binlogFilename(getBinlogFilename(header))
            .position(getPosition(header))
            .build();

        listener.onQuery(binlogEvent);
    }

    /**
     * 处理行查询事件
     */
    private void handleRowsQuery(Event event, BinlogEventListener listener) throws Exception {
        RowsQueryEventData data = event.getData();
        EventHeader header = event.getHeader();

        BinlogEvent binlogEvent = BinlogEvent.builder()
            .eventType(BinlogEvent.EventType.QUERY)
            .operation(BinlogEvent.OperationType.UNKNOWN)
            .timestamp(getTimestamp(header))
            .binlogFilename(getBinlogFilename(header))
            .position(getPosition(header))
            .build();

        listener.onQuery(binlogEvent);
    }

    /**
     * 获取事件时间戳
     */
    private java.time.LocalDateTime getTimestamp(EventHeader header) {
        if (header instanceof EventHeaderV4) {
            EventHeaderV4 headerV4 = (EventHeaderV4) header;
            return java.time.LocalDateTime.ofEpochSecond(headerV4.getTimestamp() / 1000, 0,
                java.time.ZoneOffset.ofHours(8));
        }
        return java.time.LocalDateTime.now();
    }

    /**
     * 获取binlog文件名
     */
    private String getBinlogFilename(EventHeader header) {
        // 需要从特定的事件类型中获取
        return "";
    }

    /**
     * 获取位置
     */
    private long getPosition(EventHeader header) {
        if (header instanceof EventHeaderV4) {
            EventHeaderV4 headerV4 = (EventHeaderV4) header;
            return headerV4.getNextPosition();
        }
        return 0;
    }
}
