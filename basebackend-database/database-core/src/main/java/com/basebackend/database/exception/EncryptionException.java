package com.basebackend.database.exception;

/**
 * 加密解密异常
 * 当数据加密或解密过程中出现错误时抛出
 */
public class EncryptionException extends RuntimeException {
    
    public EncryptionException(String message) {
        super(message);
    }
    
    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
