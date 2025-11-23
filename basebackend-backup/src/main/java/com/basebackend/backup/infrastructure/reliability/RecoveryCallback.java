package com.basebackend.backup.infrastructure.reliability;

/**
 * 恢复回调接口
 * 定义重试失败后的恢复操作
 */
@FunctionalInterface
public interface RecoveryCallback<T> {

    /**
     * 执行恢复操作
     *
     * @param lastException 最后一次失败的异常
     * @return 恢复结果
     * @throws Exception 恢复失败时抛出异常
     */
    T recover(Exception lastException) throws Exception;
}
