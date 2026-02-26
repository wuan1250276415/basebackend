package com.basebackend.websocket.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
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
 *   <li>在线/离线状态查询</li>
 *   <li>按用户ID发送消息</li>
 *   <li>广播消息到所有在线用户</li>
 * </ul>
 * 线程安全。
 */
@Slf4j
public class SessionManager {

    /** sessionId → SessionInfo */
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    /** userId → Set<sessionId> (一个用户可有多个设备连接) */
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    /**
     * 注册连接
     */
    public void register(WebSocketSession session, String userId) {
        String sessionId = session.getId();
        sessions.put(sessionId, new SessionInfo(session, userId, Instant.now()));
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        log.info("WebSocket 连接注册: sessionId={}, userId={}, 当前在线={}", sessionId, userId, sessions.size());
    }

    /**
     * 注销连接
     */
    public void unregister(String sessionId) {
        SessionInfo info = sessions.remove(sessionId);
        if (info != null) {
            Set<String> userSessionSet = userSessions.get(info.userId());
            if (userSessionSet != null) {
                userSessionSet.remove(sessionId);
                if (userSessionSet.isEmpty()) {
                    userSessions.remove(info.userId());
                }
            }
            log.info("WebSocket 连接注销: sessionId={}, userId={}, 当前在线={}", sessionId, info.userId(), sessions.size());
        }
    }

    /**
     * 发送消息给指定用户
     *
     * @param userId  目标用户 ID
     * @param message 文本消息
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
     * @param message 文本消息
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
     * 发送消息到指定会话
     */
    public boolean sendToSession(String sessionId, String message) {
        SessionInfo info = sessions.get(sessionId);
        if (info == null || !info.session().isOpen()) {
            return false;
        }
        try {
            info.session().sendMessage(new org.springframework.web.socket.TextMessage(message));
            return true;
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

    /**
     * 获取所有在线用户 ID
     */
    public Set<String> getOnlineUserIds() {
        return Set.copyOf(userSessions.keySet());
    }

    /**
     * 获取在线连接数
     */
    public int getConnectionCount() {
        return sessions.size();
    }

    /**
     * 获取在线用户数
     */
    public int getOnlineUserCount() {
        return userSessions.size();
    }

    /**
     * 获取指定用户的所有会话
     */
    public Set<String> getUserSessionIds(String userId) {
        Set<String> ids = userSessions.get(userId);
        return ids != null ? Set.copyOf(ids) : Set.of();
    }

    /**
     * 获取会话信息
     */
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
                try {
                    info.session().close(CloseStatus.NORMAL);
                } catch (IOException e) {
                    log.warn("关闭连接失败: sessionId={}", sessionId, e);
                }
            }
            unregister(sessionId);
        }
    }

    /**
     * 会话信息
     */
    public record SessionInfo(WebSocketSession session, String userId, Instant connectedAt) {}
}
