package com.basebackend.backup.infrastructure.executor;

import java.util.List;

/**
 * Binlog事件监听器接口
 * 用于处理从MySQL binlog中解析出的数据变更事件
 */
public interface BinlogEventListener {

    /**
     * 处理数据变更事件
     *
     * @param event 变更事件
     * @throws Exception 处理失败时抛出异常
     */
    void onDataChange(BinlogEvent event) throws Exception;

    /**
     * 处理查询事件
     *
     * @param event 查询事件
     * @throws Exception 处理失败时抛出异常
     */
    void onQuery(BinlogEvent event) throws Exception;

    /**
     * 处理DDL语句事件
     *
     * @param event DDL事件
     * @throws Exception 处理失败时抛出异常
     */
    void onDDL(BinlogEvent event) throws Exception;

    /**
     * 处理错误事件
     *
     * @param error 错误信息
     * @throws Exception 处理失败时抛出异常
     */
    void onError(Throwable error) throws Exception;

    /**
     * 获取当前收集的事件列表
     *
     * @return 事件列表
     */
    List<BinlogEvent> getEvents();
}
