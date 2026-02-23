package com.basebackend.security.event;

/**
 * 强制登出事件
 */
public class ForceLogoutEvent extends SecurityEvent {

    private final String userId;

    public ForceLogoutEvent(String userId) {
        super("TokenBlacklistService");
        this.userId = userId;
    }

    @Override
    public String getEventType() {
        return "FORCE_LOGOUT";
    }

    public String getUserId() {
        return userId;
    }
}
