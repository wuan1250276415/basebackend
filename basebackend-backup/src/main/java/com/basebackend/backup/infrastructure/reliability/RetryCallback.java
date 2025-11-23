package com.basebackend.backup.infrastructure.reliability;

/**
 * 重试回调接口
 * 定义需要重试的操作
 */
@FunctionalInterface
public interface RetryCallback<T> {

    /**
     * 执行需要重试的操作
     *
     * @return 操作结果
     * @throws Exception 操作失败时抛出异常
     */
    T doWithRetry() throws Exception;
}
