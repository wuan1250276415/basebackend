package com.basebackend.database.exception;

/**
 * 数据源异常
 * 当数据源连接、切换或配置出现问题时抛出
 */
public class DataSourceException extends RuntimeException {
    
    public DataSourceException(String message) {
        super(message);
    }
    
    public DataSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
