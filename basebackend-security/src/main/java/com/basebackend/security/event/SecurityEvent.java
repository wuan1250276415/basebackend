package com.basebackend.security.event;

import java.time.Instant;

/**
 * 安全事件基类
 */
public abstract class SecurityEvent {

    private final Instant timestamp;
    private final String source;

    protected SecurityEvent(String source) {
        this.timestamp = Instant.now();
        this.source = source;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getSource() {
        return source;
    }

    /**
     * 事件类型标识，子类覆写
     */
    public abstract String getEventType();
}
