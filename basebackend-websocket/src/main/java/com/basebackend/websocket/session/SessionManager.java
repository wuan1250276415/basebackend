package com.basebackend.websocket.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.SessionLimitExceededException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * WebSocket 会话管理器
 * <p>
 * 管理所有在线用户的 WebSocket 连接，支持：
 * <ul>
 *   <li>用户级连接管理（一个用户可有多个连接）</li>
 *   <li>全局及单用户连接数限制</li>
 *   <li>在线/离线状态查询</li>
 *   <li>按用户ID发送消息（线程安全）</li>
 *   <li>广播消息到所有在线用户</li>
 *   <li>心跳超时会话检测</li>
 * </ul>
 * 线程安全：通过 {@link ConcurrentWebSocketSessionDecorator} 保证并发发送安全。
 */
@Slf4j
public class SessionManager {

    /** sessionId → SessionInfo（持有 ConcurrentWebSocketSessionDecorator） */
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    /** userId → Set<sessionId>（一个用户可有多个设备连接） */
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    /** sessionId → 最近活跃时间（用于心跳超时检测） */
    private final Map<String, Instant> lastActiveAt = new ConcurrentHashMap<>();

    private final int maxConnections;
    private final int maxUserConnections;
    private final int sendTimeLimitMs;
    private final int sendBufferSizeBytes;

    public SessionManager(int maxConnections, int maxUserConnections,
                          int sendTimeLimitMs, int sendBufferSizeKb) {
        this.maxConnections = maxConnections;
        this.maxUserConnections = maxUserConnections;
        this.sendTimeLimitMs = sendTimeLimitMs;
        this.sendBufferSizeBytes = sendBufferSizeKb * 1024;
    }

    /** 默认构造（无限制模式，主要供测试使用） */
    public SessionManager() {
        this(10_000, 5, 5_000, 512);
    }

    /**
     * 注册连接
     *
     * @return true 注册成功；false 超过连接限制，连接已被关闭
     */
    public boolean register(WebSocketSession rawSession, String userId) {
        // 全局连接数检查
        if (sessions.size() >= maxConnections) {
            log.warn("全局连接数已达上限 {}, 拒绝新连接: sessionId={}", maxConnections, rawSession.getId());
            closeQuietly(rawSession, CloseStatus.SERVICE_OVERLOAD);
            return false;
        }

        // 单用户连接数检查
        Set<String> existing = userSessions.getOrDefault(userId, Set.of());
        if (existing.size() >= maxUserConnections) {
            log.warn("用户连接数已达上限 {}: userId={}", maxUserConnections, userId);
            closeQuietly(rawSession, CloseStatus.SERVICE_OVERLOAD);
            return false;
        }

        // 用线程安全装饰器包装，防止并发 sendMessage 异常
        ConcurrentWebSocketSessionDecorator safeSession =
                new ConcurrentWebSocketSessionDecorator(rawSession, sendTimeLimitMs, sendBufferSizeBytes);

        String sessionId = rawSession.getId();
        sessions.put(sessionId, new SessionInfo(safeSession, userId, Instant.now()));
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        lastActiveAt.put(sessionId, Instant.now());

        log.info("WebSocket 连接注册: sessionId={}, userId={}, 当前在线={}",
                sessionId, maskId(userId), sessions.size());
        return true;
    }

    /**
     * 注销连接
     * <p>
     * 使用 {@link java.util.concurrent.ConcurrentHashMap#computeIfPresent} 原子性地
     * 从反向索引中移除 sessionId，避免"检查-然后-删除"竞态导致新注册会话被误清除。
     */
    public void unregister(String sessionId) {
        SessionInfo info = sessions.remove(sessionId);
        lastActiveAt.remove(sessionId);
        if (info == null) return;

        // computeIfPresent 是 ConcurrentHashMap 的原子操作：
        // 在持有桶锁期间完成 remove + isEmpty 检查，防止并发注册时误删非空 Set
        userSessions.computeIfPresent(info.userId(), (uid, sessionSet) -> {
            sessionSet.remove(sessionId);
            return sessionSet.isEmpty() ? null : sessionSet;
        });

        log.info("WebSocket 连接注销: sessionId={}, userId={}, 当前在线={}",
                sessionId, maskId(info.userId()), sessions.size());
    }

    /**
     * 更新会话最近活跃时间（每次收到消息时调用）
     */
    public void updateLastActive(String sessionId) {
        if (sessions.containsKey(sessionId)) {
            lastActiveAt.put(sessionId, Instant.now());
        }
    }

    /**
     * 获取超过指定超时时间未活跃的会话 ID 列表
     */
    public List<String> getStaleSessionIds(Duration timeout) {
        Instant cutoff = Instant.now().minus(timeout);
        return lastActiveAt.entrySet().stream()
                .filter(e -> e.getValue().isBefore(cutoff))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 主动断开指定会话（心跳超时时调用）
     */
    public void disconnectSession(String sessionId) {
        SessionInfo info = sessions.get(sessionId);
        if (info != null && info.session().isOpen()) {
            closeQuietly(info.session(), CloseStatus.SESSION_NOT_RELIABLE);
        }
        unregister(sessionId);
    }

    /**
     * 发送消息给指定用户的所有连接
     *
     * @return 成功发送的连接数
     */
    public int sendToUser(String userId, String message) {
        Set<String> sessionIds = userSessions.get(userId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            log.debug("用户不在线: userId={}", userId);
            return 0;
        }
        int sent = 0;
        for (String sessionId : sessionIds) {
            if (sendToSession(sessionId, message)) {
                sent++;
            }
        }
        return sent;
    }

    /**
     * 广播消息到所有在线用户
     *
     * @return 成功发送的连接数
     */
    public int broadcast(String message) {
        int sent = 0;
        for (String sessionId : sessions.keySet()) {
            if (sendToSession(sessionId, message)) {
                sent++;
            }
        }
        log.debug("广播消息: 发送到 {}/{} 个连接", sent, sessions.size());
        return sent;
    }

    /**
     * 发送消息到指定会话（线程安全，并发调用安全）
     */
    public boolean sendToSession(String sessionId, String message) {
        SessionInfo info = sessions.get(sessionId);
        if (info == null || !info.session().isOpen()) {
            return false;
        }
        try {
            info.session().sendMessage(new org.springframework.web.socket.TextMessage(message));
            return true;
        } catch (SessionLimitExceededException e) {
            log.warn("发送超限，关闭连接: sessionId={}", sessionId);
            unregister(sessionId);
            return false;
        } catch (IOException e) {
            log.warn("发送消息失败: sessionId={}, error={}", sessionId, e.getMessage());
            return false;
        }
    }

    /**
     * 用户是否在线
     */
    public boolean isOnline(String userId) {
        Set<String> sessionIds = userSessions.get(userId);
        return sessionIds != null && !sessionIds.isEmpty();
    }

    /** 获取所有在线用户 ID（不可变副本） */
    public Set<String> getOnlineUserIds() {
        return Set.copyOf(userSessions.keySet());
    }

    /** 获取在线连接总数 */
    public int getConnectionCount() {
        return sessions.size();
    }

    /** 获取在线用户数 */
    public int getOnlineUserCount() {
        return userSessions.size();
    }

    /** 获取指定用户的所有会话 ID（不可变副本） */
    public Set<String> getUserSessionIds(String userId) {
        Set<String> ids = userSessions.get(userId);
        return ids != null ? Set.copyOf(ids) : Set.of();
    }

    /** 获取会话信息 */
    public SessionInfo getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 关闭指定用户的所有连接
     */
    public void disconnectUser(String userId) {
        Set<String> sessionIds = userSessions.get(userId);
        if (sessionIds == null) return;

        for (String sessionId : new ArrayList<>(sessionIds)) {
            SessionInfo info = sessions.get(sessionId);
            if (info != null && info.session().isOpen()) {
                closeQuietly(info.session(), CloseStatus.NORMAL);
            }
            unregister(sessionId);
        }
    }

    // --- 内部工具 ---

    /**
     * 日志脱敏：保留前 2 位可读字符，其余替换为 *。
     * 防止 userId（可能含手机号、邮箱等敏感信息）直接出现在日志中。
     */
    public static String maskId(String id) {
        if (id == null || id.length() <= 2) return "**";
        return id.substring(0, 2) + "*".repeat(Math.min(id.length() - 2, 6));
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException e) {
            log.warn("关闭连接失败: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 会话信息（不可变）
     *
     * @param session     线程安全的 WebSocket 会话
     * @param userId      关联用户 ID
     * @param connectedAt 连接建立时间
     */
    public record SessionInfo(WebSocketSession session, String userId, Instant connectedAt) {}
}
