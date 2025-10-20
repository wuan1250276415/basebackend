package com.basebackend.messaging.exception;

/**
 * 消息异常基类
 */
public class MessagingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MessagingException(String message) {
        super(message);
    }

    public MessagingException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessagingException(Throwable cause) {
        super(cause);
    }
}
