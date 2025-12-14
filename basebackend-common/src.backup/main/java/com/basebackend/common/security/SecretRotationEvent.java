package com.basebackend.common.security;

import org.springframework.context.ApplicationEvent;

/**
 * 密钥轮换事件
 */
public class SecretRotationEvent extends ApplicationEvent {

    private final String key;

    public SecretRotationEvent(Object source, String key) {
        super(source);
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
