package com.basebackend.common.idempotent.store;

import java.util.concurrent.TimeUnit;

/**
 * 幂等存储 SPI 接口
 * <p>
 * 定义幂等性检查的存储操作契约，支持不同的底层实现（Redis、内存等）。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public interface IdempotentStore {

    /**
     * 尝试占位（原子操作）
     *
     * @param key     幂等 key
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true 表示首次请求（占位成功），false 表示重复请求
     */
    boolean tryAcquire(String key, long timeout, TimeUnit unit);

    /**
     * 释放占位
     *
     * @param key 幂等 key
     */
    void release(String key);
}
