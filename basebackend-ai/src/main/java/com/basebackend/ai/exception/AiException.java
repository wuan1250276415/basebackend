package com.basebackend.ai.exception;

/**
 * AI 模块基础异常
 */
public class AiException extends RuntimeException {

    private final String provider;
    private final int statusCode;

    public AiException(String message) {
        this(message, null, 0);
    }

    public AiException(String message, Throwable cause) {
        this(message, null, 0, cause);
    }

    public AiException(String message, String provider, int statusCode) {
        super(message);
        this.provider = provider;
        this.statusCode = statusCode;
    }

    public AiException(String message, String provider, int statusCode, Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.statusCode = statusCode;
    }

    public String getProvider() {
        return provider;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
