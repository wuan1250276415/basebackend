package com.basebackend.backup.infrastructure.executor;

import java.util.List;

/**
 * PostgreSQL WAL事件监听器接口
 */
public interface WalEventListener {

    /**
     * 处理数据变更事件
     *
     * @param event WAL事件
     * @throws Exception 处理失败时抛出异常
     */
    void onDataChange(WalEvent event) throws Exception;

    /**
     * 处理DDL事件
     *
     * @param event DDL事件
     * @throws Exception 处理失败时抛出异常
     */
    void onDDL(WalEvent event) throws Exception;

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
    List<WalEvent> getEvents();
}
