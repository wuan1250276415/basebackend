package com.basebackend.websocket.handler;

import com.basebackend.websocket.channel.ChannelManager;
import com.basebackend.websocket.session.SessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;

/**
 * 核心 WebSocket 消息处理器
 * <p>
 * 处理连接建立/关闭、消息路由、心跳检测等核心逻辑。
 * 支持以下消息类型：
 * <ul>
 *   <li>{@code {"type":"text","to":"userId","content":"hello"}} — 私聊</li>
 *   <li>{@code {"type":"channel","channel":"room1","content":"hi"}} — 频道消息</li>
 *   <li>{@code {"type":"join","channel":"room1"}} — 加入频道</li>
 *   <li>{@code {"type":"leave","channel":"room1"}} — 离开频道</li>
 *   <li>{@code {"type":"ping"}} — 心跳</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class WsMessageHandler extends TextWebSocketHandler {

    private final SessionManager sessionManager;
    private final ChannelManager channelManager;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = extractUserId(session);
        if (userId == null) {
            log.warn("WebSocket 连接缺少用户标识, sessionId={}", session.getId());
            userId = "anonymous-" + session.getId().substring(0, 8);
        }
        sessionManager.register(session, userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        SessionManager.SessionInfo info = sessionManager.getSession(session.getId());
        if (info == null) return;

        String userId = info.userId();

        try {
            JsonNode node = objectMapper.readTree(payload);
            String type = node.path("type").asText("text");

            switch (type) {
                case "text" -> handlePrivateMessage(userId, node);
                case "channel" -> handleChannelMessage(userId, node);
                case "join" -> handleJoinChannel(userId, node);
                case "leave" -> handleLeaveChannel(userId, node);
                case "ping" -> handlePing(session);
                case "broadcast" -> handleBroadcast(userId, node);
                default -> log.debug("未知消息类型: type={}, userId={}", type, userId);
            }
        } catch (Exception e) {
            log.warn("处理消息异常: userId={}, error={}", userId, e.getMessage());
            sessionManager.sendToSession(session.getId(),
                    """
                    {"type":"error","content":"消息格式错误: %s"}""".formatted(e.getMessage()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SessionManager.SessionInfo info = sessionManager.getSession(session.getId());
        if (info != null) {
            channelManager.leaveAll(info.userId());
        }
        sessionManager.unregister(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket 传输错误: sessionId={}, error={}", session.getId(), exception.getMessage());
        SessionManager.SessionInfo info = sessionManager.getSession(session.getId());
        if (info != null) {
            channelManager.leaveAll(info.userId());
        }
        sessionManager.unregister(session.getId());
    }

    // --- 消息处理 ---

    private void handlePrivateMessage(String fromUserId, JsonNode node) {
        String toUserId = node.path("to").asText();
        String content = node.path("content").asText();

        String msg = """
                {"type":"text","from":"%s","content":"%s","timestamp":%d}"""
                .formatted(fromUserId, escapeJson(content), System.currentTimeMillis());

        int sent = sessionManager.sendToUser(toUserId, msg);
        log.debug("私聊消息: from={}, to={}, sent={}", fromUserId, toUserId, sent);
    }

    private void handleChannelMessage(String fromUserId, JsonNode node) {
        String channelId = node.path("channel").asText();
        String content = node.path("content").asText();

        if (!channelManager.isMember(channelId, fromUserId)) {
            sessionManager.sendToUser(fromUserId,
                    """
                    {"type":"error","content":"未加入频道: %s"}""".formatted(channelId));
            return;
        }

        String msg = """
                {"type":"channel","from":"%s","channel":"%s","content":"%s","timestamp":%d}"""
                .formatted(fromUserId, channelId, escapeJson(content), System.currentTimeMillis());

        Set<String> members = channelManager.getMembers(channelId);
        int sent = 0;
        for (String memberId : members) {
            if (!memberId.equals(fromUserId)) {
                sent += sessionManager.sendToUser(memberId, msg);
            }
        }
        log.debug("频道消息: from={}, channel={}, members={}, sent={}", fromUserId, channelId, members.size(), sent);
    }

    private void handleJoinChannel(String userId, JsonNode node) {
        String channelId = node.path("channel").asText();
        channelManager.join(channelId, userId);

        // 通知频道内其他成员
        String msg = """
                {"type":"join","userId":"%s","channel":"%s","timestamp":%d}"""
                .formatted(userId, channelId, System.currentTimeMillis());

        for (String memberId : channelManager.getMembers(channelId)) {
            if (!memberId.equals(userId)) {
                sessionManager.sendToUser(memberId, msg);
            }
        }
    }

    private void handleLeaveChannel(String userId, JsonNode node) {
        String channelId = node.path("channel").asText();
        channelManager.leave(channelId, userId);

        String msg = """
                {"type":"leave","userId":"%s","channel":"%s","timestamp":%d}"""
                .formatted(userId, channelId, System.currentTimeMillis());

        for (String memberId : channelManager.getMembers(channelId)) {
            sessionManager.sendToUser(memberId, msg);
        }
    }

    private void handlePing(WebSocketSession session) {
        sessionManager.sendToSession(session.getId(),
                """
                {"type":"pong","timestamp":%d}""".formatted(System.currentTimeMillis()));
    }

    private void handleBroadcast(String fromUserId, JsonNode node) {
        String content = node.path("content").asText();
        String msg = """
                {"type":"broadcast","from":"%s","content":"%s","timestamp":%d}"""
                .formatted(fromUserId, escapeJson(content), System.currentTimeMillis());
        sessionManager.broadcast(msg);
    }

    // --- 工具方法 ---

    private String extractUserId(WebSocketSession session) {
        // 从 URI 参数或 Header 中提取用户 ID
        Map<String, Object> attrs = session.getAttributes();
        Object userId = attrs.get("userId");
        if (userId != null) return userId.toString();

        // 从 query parameter 提取
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null && query.contains("userId=")) {
            for (String param : query.split("&")) {
                if (param.startsWith("userId=")) {
                    return param.substring(7);
                }
            }
        }

        return null;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r");
    }
}
