package com.basebackend.security.exception;

/**
 * Token黑名单服务异常
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-12-08
 */
public class TokenBlacklistException extends RuntimeException {

    public TokenBlacklistException(String message) {
        super(message);
    }

    public TokenBlacklistException(String message, Throwable cause) {
        super(message, cause);
    }
}
