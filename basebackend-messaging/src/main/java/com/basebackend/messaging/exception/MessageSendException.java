package com.basebackend.messaging.exception;

/**
 * 消息发送异常
 */
public class MessageSendException extends MessagingException {

    private static final long serialVersionUID = 1L;

    public MessageSendException(String message) {
        super(message);
    }

    public MessageSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
