package com.basebackend.database.exception;

/**
 * 租户上下文异常
 * 当租户上下文缺失或无效时抛出
 */
public class TenantContextException extends RuntimeException {
    
    public TenantContextException(String message) {
        super(message);
    }
    
    public TenantContextException(String message, Throwable cause) {
        super(message, cause);
    }
}
