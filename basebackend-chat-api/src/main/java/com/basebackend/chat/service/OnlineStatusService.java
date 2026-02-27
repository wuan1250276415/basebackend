package com.basebackend.chat.service;

import java.util.List;
import java.util.Map;

/**
 * 在线状态服务接口
 * <p>
 * 管理用户在线/离线/忙碌状态，基于 Redis SET + STRING 存储。
 * 支持多端登录场景：同一用户多个 WebSocket 连接共享在线状态。
 */
public interface OnlineStatusService {

    /**
     * 用户上线 — 注册到 Redis 并广播给好友
     *
     * @param tenantId 租户ID
     * @param userId   用户ID
     */
    void userOnline(Long tenantId, Long userId);

    /**
     * 用户下线 — 仅当该用户无其他活跃会话时才标记离线
     *
     * @param tenantId 租户ID
     * @param userId   用户ID
     * @param hasOtherSessions 该用户是否还有其他活跃連接
     */
    void userOffline(Long tenantId, Long userId, boolean hasOtherSessions);

    /**
     * 设置用户状态 (online / busy / away 等)
     */
    void setStatus(Long tenantId, Long userId, String status);

    /**
     * 获取单个用户状态
     */
    String getStatus(Long tenantId, Long userId);

    /**
     * 批量获取用户在线状态
     *
     * @param tenantId 租户ID
     * @param userIds  用户ID列表
     * @return userId -> {status, lastActive}
     */
    Map<String, Map<String, Object>> batchGetStatus(Long tenantId, List<Long> userIds);

    /**
     * 判断用户是否在线
     */
    boolean isOnline(Long tenantId, Long userId);
}
