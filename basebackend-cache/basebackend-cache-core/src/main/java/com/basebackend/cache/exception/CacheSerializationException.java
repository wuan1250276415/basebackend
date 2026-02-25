package com.basebackend.cache.exception;

/**
 * 缓存序列化异常
 */
public class CacheSerializationException extends CacheException {
    
    public CacheSerializationException(String message) {
        super(message);
    }
    
    public CacheSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CacheSerializationException(Throwable cause) {
        super(cause);
    }
}
