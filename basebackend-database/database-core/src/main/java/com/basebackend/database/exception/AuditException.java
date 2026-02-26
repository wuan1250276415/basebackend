package com.basebackend.database.exception;

/**
 * 审计异常
 * 当审计日志记录或查询过程中出现错误时抛出
 */
public class AuditException extends RuntimeException {
    
    public AuditException(String message) {
        super(message);
    }
    
    public AuditException(String message, Throwable cause) {
        super(message, cause);
    }
}
