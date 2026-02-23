package com.basebackend.security.event;

/**
 * 登录失败事件（触发限流封禁时发布）
 */
public class LoginFailedEvent extends SecurityEvent {

    private final String identifier;
    private final int attemptCount;
    private final boolean blocked;

    public LoginFailedEvent(String identifier, int attemptCount, boolean blocked) {
        super("AuthenticationRateLimiter");
        this.identifier = identifier;
        this.attemptCount = attemptCount;
        this.blocked = blocked;
    }

    @Override
    public String getEventType() {
        return "LOGIN_FAILED";
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public boolean isBlocked() {
        return blocked;
    }
}
