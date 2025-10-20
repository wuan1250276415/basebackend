package com.basebackend.messaging.exception;

/**
 * 消息消费异常
 */
public class MessageConsumeException extends MessagingException {

    private static final long serialVersionUID = 1L;

    public MessageConsumeException(String message) {
        super(message);
    }

    public MessageConsumeException(String message, Throwable cause) {
        super(message, cause);
    }
}
