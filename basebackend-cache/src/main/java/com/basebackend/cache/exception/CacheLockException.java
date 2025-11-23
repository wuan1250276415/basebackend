package com.basebackend.cache.exception;

/**
 * 缓存锁异常
 * 当分布式锁操作失败时抛出
 */
public class CacheLockException extends CacheException {

    public CacheLockException(String message) {
        super(message);
    }

    public CacheLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
