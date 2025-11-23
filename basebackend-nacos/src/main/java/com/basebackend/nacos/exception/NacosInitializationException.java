package com.basebackend.nacos.exception;

/**
 * Nacos初始化异常
 */
public class NacosInitializationException extends RuntimeException {

    public NacosInitializationException(String message) {
        super(message);
    }

    public NacosInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
