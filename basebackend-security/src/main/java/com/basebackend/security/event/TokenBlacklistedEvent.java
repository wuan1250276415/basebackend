package com.basebackend.security.event;

/**
 * Token 加入黑名单事件
 */
public class TokenBlacklistedEvent extends SecurityEvent {

    private final String tokenHash;
    private final String userId;

    public TokenBlacklistedEvent(String tokenHash, String userId) {
        super("TokenBlacklistService");
        this.tokenHash = tokenHash;
        this.userId = userId;
    }

    @Override
    public String getEventType() {
        return "TOKEN_BLACKLISTED";
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getUserId() {
        return userId;
    }
}
