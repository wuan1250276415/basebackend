package com.basebackend.backup.infrastructure.executor.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.infrastructure.executor.BinlogEvent;
import com.basebackend.backup.infrastructure.executor.BinlogEventListener;
import com.basebackend.backup.infrastructure.executor.BinlogPosition;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MySQL Binlog解析器
 * 使用mysql-binlog-connector-java库实现对MySQL binlog的实时解析
 * <p>
 * 核心功能：
 * 1. 获取当前binlog位置
 * 2. 订阅binlog变更事件
 * 3. 解析binlog事件并转换为标准格式
 * 4. 支持超时自动断开连接，防止资源泄漏
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MySqlBinlogParser {

    private final BackupProperties backupProperties;
    private BinaryLogClient client;
    private boolean isConnected = false;
    private ScheduledExecutorService timeoutScheduler = Executors.newScheduledThreadPool(1);

    /**
     * 获取当前binlog位置
     * <p>
     * 通过查询MySQL的SHOW MASTER STATUS命令获取当前binlog文件名和位置
     * 这是增量备份的起始点，用于确定从哪个位置开始解析binlog
     *
     * @param host MySQL主机地址
     * @param port MySQL端口号
     * @param username 数据库用户名
     * @param password 数据库密码
     * @return 当前binlog位置信息，包含文件名和偏移量
     * @throws Exception 获取失败时抛出异常（如网络异常、权限不足等）
     */
    public BinlogPosition getCurrentPosition(String host, int port, String username, String password) throws Exception {
        // TODO: 实现真正的SHOW MASTER STATUS查询
        // 需要通过JDBC连接查询，避免依赖外部命令
        log.info("获取MySQL当前binlog位置: {}:{}", host, port);

        // 当前返回空位置，表示从头开始解析
        // 实际实现中应该查询SHOW MASTER STATUS并返回真实的binlog位置
        return BinlogPosition.of("", 4);
    }

    /**
     * 订阅MySQL binlog变更事件
     * <p>
     * 建立与MySQL的binlog监听连接，实时接收数据库变更事件。
     * 支持监听INSERT、UPDATE、DELETE等DML操作以及DDL语句。
     * <p>
     * 重要说明：
     * 1. 该方法会阻塞当前线程，直到连接断开
     * 2. 连接支持自动超时断开（默认10分钟）
     * 3. 事件监听器会在独立线程中调用，注意线程安全问题
     *
     * @param host MySQL服务器主机地址
     * @param port MySQL服务器端口号
     * @param username 具有REPLICATION SLAVE权限的用户名
     * @param password 用户密码
     * @param startPosition 起始binlog位置，如果为null则从当前位置开始
     * @param listener 事件监听器，用于处理接收到的binlog事件
     * @throws Exception 建立连接或监听过程中发生的异常
     */
    public void subscribe(String host, int port, String username, String password,
                         BinlogPosition startPosition, BinlogEventListener listener) throws Exception {
        log.info("开始订阅MySQL binlog变更事件，起始位置: {}", startPosition);

        // 创建BinaryLogClient实例，用于连接MySQL服务器
        client = new BinaryLogClient(host, port, username, password);

        // 设置起始位置
        if (startPosition != null && startPosition.isValid()) {
            client.setBinlogFilename(startPosition.getFilename());
            client.setBinlogPosition(startPosition.getPosition());
        }

        // 注册binlog事件监听器
        // 所有接收到的binlog事件都会触发此监听器
        client.registerEventListener(event -> {
            try {
                EventHeader header = event.getHeader();
                EventType eventType = header.getEventType();

                // 记录调试日志，注意在生产环境中建议使用TRACE级别避免日志过多
                log.trace("接收到binlog事件: {}", eventType);

                // 根据事件类型分发处理逻辑
                switch (eventType) {
                    case WRITE_ROWS:
                    case EXT_WRITE_ROWS:
                        // 处理INSERT操作事件
                        handleWriteRows(event, listener);
                        break;

                    case UPDATE_ROWS:
                    case EXT_UPDATE_ROWS:
                        // 处理UPDATE操作事件
                        handleUpdateRows(event, listener);
                        break;

                    case DELETE_ROWS:
                    case EXT_DELETE_ROWS:
                        // 处理DELETE操作事件
                        handleDeleteRows(event, listener);
                        break;

                    case QUERY:
                        // 处理SQL语句事件（包括DDL）
                        handleQuery(event, listener);
                        break;

                    case ROWS_QUERY:
                        // 处理行查询事件
                        handleRowsQuery(event, listener);
                        break;

                    default:
                        // 对于不支持的事件类型，记录debug日志但不中断处理
                        log.trace("忽略不支持的binlog事件类型: {}", eventType);
                        break;
                }
            } catch (Exception e) {
                // 记录处理异常，但不让异常中断事件监听循环
                log.error("处理binlog事件时发生异常", e);
                try {
                    // 通知监听器发生错误
                    listener.onError(e);
                } catch (Exception ex) {
                    // 监听器错误处理也失败，记录日志但不传播异常
                    log.error("事件监听器的错误处理器执行失败", ex);
                }
            }
        });

        // 连接并开始监听
        client.connect();
        isConnected = true;

        // 设置超时自动断开（默认10分钟）
        timeoutScheduler.schedule(() -> {
            if (isConnected) {
                log.warn("Binlog订阅超时，自动断开连接");
                disconnect();
            }
        }, 10, TimeUnit.MINUTES);

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
     * 断开binlog监听连接
     * <p>
     * 安全地关闭与MySQL服务器的binlog监听连接，包括：
     * 1. 断开BinaryLogClient连接
     * 2. 停止超时调度器
     * 3. 重置连接状态标志
     * <p>
     * 此方法可以在异常情况下调用，是幂等的（多次调用是安全的）
     */
    public void disconnect() {
        if (client != null && isConnected) {
            try {
                // 断开与MySQL的binlog连接
                client.disconnect();
                isConnected = false;

                // 取消并清理超时调度器
                timeoutScheduler.shutdownNow();
                if (!timeoutScheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    log.warn("超时调度器未能在1秒内关闭，强制关闭");
                    timeoutScheduler.shutdownNow();
                }

                log.info("MySQL binlog监听连接已安全断开");
            } catch (IOException e) {
                log.error("断开MySQL binlog连接时发生异常", e);
            } catch (InterruptedException e) {
                log.error("等待超时调度器关闭时被中断", e);
                Thread.currentThread().interrupt();
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
