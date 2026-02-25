package com.basebackend.database.exception;

/**
 * 数据库迁移异常
 * 
 * @author basebackend
 */
public class MigrationException extends RuntimeException {

    public MigrationException(String message) {
        super(message);
    }

    public MigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
