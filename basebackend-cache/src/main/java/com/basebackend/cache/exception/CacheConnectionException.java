package com.basebackend.cache.exception;

/**
 * 缓存连接异常
 * 当 Redis 连接失败或超时时抛出
 */
public class CacheConnectionException extends CacheException {
    
    public CacheConnectionException(String message) {
        super(message);
    }
    
    public CacheConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CacheConnectionException(Throwable cause) {
        super(cause);
    }
}
