package com.basebackend.cache.exception;

/**
 * 缓存配置异常
 * 当缓存配置无效或不完整时抛出
 */
public class CacheConfigurationException extends CacheException {
    
    public CacheConfigurationException(String message) {
        super(message);
    }
    
    public CacheConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CacheConfigurationException(Throwable cause) {
        super(cause);
    }
}
