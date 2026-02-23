package com.basebackend.security.event;

/**
 * 权限变更事件
 */
public class PermissionChangedEvent extends SecurityEvent {

    private final Long userId;
    private final boolean allUsersAffected;

    public PermissionChangedEvent(Long userId) {
        super("DynamicPermissionService");
        this.userId = userId;
        this.allUsersAffected = false;
    }

    public PermissionChangedEvent() {
        super("DynamicPermissionService");
        this.userId = null;
        this.allUsersAffected = true;
    }

    @Override
    public String getEventType() {
        return "PERMISSION_CHANGED";
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isAllUsersAffected() {
        return allUsersAffected;
    }
}
