package com.basebackend.observability.websocket;

import com.alibaba.fastjson2.JSON;
import com.basebackend.observability.logging.model.LogEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 实时日志流WebSocket
 */
@Slf4j
@Component
@ServerEndpoint("/ws/logs/{serviceName}")
public class LogStreamWebSocket {

    // 存储会话：serviceName -> sessions
    private static final Map<String, CopyOnWriteArraySet<Session>> SERVICE_SESSIONS = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("serviceName") String serviceName) {
        log.info("WebSocket connected: service={}, sessionId={}", serviceName, session.getId());
        
        SERVICE_SESSIONS.computeIfAbsent(serviceName, k -> new CopyOnWriteArraySet<>())
                .add(session);
        
        // 发送欢迎消息
        sendMessage(session, "Connected to log stream for service: " + serviceName);
    }

    @OnClose
    public void onClose(Session session, @PathParam("serviceName") String serviceName) {
        log.info("WebSocket disconnected: service={}, sessionId={}", serviceName, session.getId());
        
        CopyOnWriteArraySet<Session> sessions = SERVICE_SESSIONS.get(serviceName);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                SERVICE_SESSIONS.remove(serviceName);
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket error: sessionId={}", session.getId(), error);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.debug("Received message from client: {}", message);
        // 可以处理客户端发来的过滤条件等
    }

    /**
     * 广播日志到所有订阅该服务的客户端
     */
    public static void broadcastLog(String serviceName, LogEntry log) {
        CopyOnWriteArraySet<Session> sessions = SERVICE_SESSIONS.get(serviceName);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String message = JSON.toJSONString(log);
        
        for (Session session : sessions) {
            if (session.isOpen()) {
                sendMessage(session, message);
            }
        }
    }

    /**
     * 发送消息
     */
    private static void sendMessage(Session session, String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (Exception e) {
            log.error("Failed to send message to session: {}", session.getId(), e);
        }
    }

    /**
     * 获取在线连接数
     */
    public static int getOnlineCount() {
        return SERVICE_SESSIONS.values().stream()
                .mapToInt(CopyOnWriteArraySet::size)
                .sum();
    }
}
