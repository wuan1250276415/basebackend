package com.basebackend.websocket.handler;

import com.basebackend.websocket.broadcast.BroadcastStrategy;
import com.basebackend.websocket.channel.ChannelManager;
import com.basebackend.websocket.message.WsMessage;
import com.basebackend.websocket.session.SessionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 核心 WebSocket 消息处理器
 * <p>
 * 处理连接建立/关闭、消息路由、心跳检测等核心逻辑。
 * 所有出站消息均通过 {@link WsMessage} 工厂方法构造，再经 {@link ObjectMapper} 序列化，
 * 彻底消除手工拼接 JSON 导致的注入风险。
 *
 * <p>支持以下客户端消息类型：
 * <ul>
 *   <li>{@code {"type":"text","to":"userId","content":"hello"}} — 私聊</li>
 *   <li>{@code {"type":"channel","channel":"room1","content":"hi"}} — 频道消息</li>
 *   <li>{@code {"type":"join","channel":"room1"}} — 加入频道</li>
 *   <li>{@code {"type":"leave","channel":"room1"}} — 离开频道</li>
 *   <li>{@code {"type":"ping"}} — 心跳</li>
 *   <li>{@code {"type":"broadcast","content":"..."}} — 全局广播（需 session attribute {@code isAdmin=true}）</li>
 * </ul>
 */
@Slf4j
public class WsMessageHandler extends TextWebSocketHandler {

    /** 频道 ID / 用户 ID 最大长度 */
    private static final int MAX_ID_LENGTH = 128;

    /**
     * ID 合法字符白名单：字母、数字、下划线、连字符、点、冒号。
     * 明确排除 {@code /} {@code \} 等路径遍历字符。
     */
    private static final Pattern SAFE_ID_PATTERN = Pattern.compile("[a-zA-Z0-9_\\-.:]+");

    private final SessionManager sessionManager;
    private final ChannelManager channelManager;
    private final ObjectMapper objectMapper;
    private final int maxMessageSizeBytes;
    private final BroadcastStrategy broadcastStrategy;

    public WsMessageHandler(SessionManager sessionManager,
                             ChannelManager channelManager,
                             ObjectMapper objectMapper,
                             int maxMessageSizeBytes,
                             BroadcastStrategy broadcastStrategy) {
        this.sessionManager = sessionManager;
        this.channelManager = channelManager;
        this.objectMapper = objectMapper;
        this.maxMessageSizeBytes = maxMessageSizeBytes;
        this.broadcastStrategy = broadcastStrategy;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = extractUserId(session);
        if (userId == null) {
            log.warn("WebSocket 连接缺少用户标识, sessionId={}", session.getId());
            userId = "anonymous-" + session.getId();
        }
        boolean registered = sessionManager.register(session, userId);
        if (!registered) {
            log.warn("连接注册失败（超限）: sessionId={}", session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // P2-I3: 消息大小校验
        if (message.getPayloadLength() > maxMessageSizeBytes) {
            log.warn("消息超过最大长度: sessionId={}, size={}", session.getId(), message.getPayloadLength());
            sendError(session.getId(), "消息超过最大长度限制");
            return;
        }

        SessionManager.SessionInfo info = sessionManager.getSession(session.getId());
        if (info == null) return;

        String userId = info.userId();
        sessionManager.updateLastActive(session.getId());

        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            String type = node.path("type").asText("text");

            switch (type) {
                case "text"      -> handlePrivateMessage(userId, node);
                case "channel"   -> handleChannelMessage(userId, node);
                case "join"      -> handleJoinChannel(userId, node);
                case "leave"     -> handleLeaveChannel(userId, node);
                case "ping"      -> handlePing(session);
                case "broadcast" -> handleBroadcast(session, userId, node);
                default          -> log.debug("未知消息类型: type={}, userId={}", type, maskId(userId));
            }
        } catch (JsonProcessingException e) {
            log.warn("消息 JSON 解析失败: userId={}, type={}", maskId(userId), e.getClass().getSimpleName());
            sendError(session.getId(), "消息格式错误，请检查 JSON 格式");
        } catch (Exception e) {
            log.warn("处理消息异常: userId={}, type={}", maskId(userId), e.getClass().getSimpleName(), e);
            sendError(session.getId(), "服务器内部错误");
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

    private void handlePrivateMessage(String fromUserId, JsonNode node) throws JsonProcessingException {
        String toUserId = node.path("to").asText();
        if (!isSafeId(toUserId)) {
            log.debug("私聊目标 userId 不合法: to={}", toUserId);
            return;
        }
        String content = node.path("content").asText();
        int sent = sessionManager.sendToUser(toUserId,
                objectMapper.writeValueAsString(WsMessage.text(fromUserId, toUserId, content)));
        log.debug("私聊: from={}, to={}, sent={}", maskId(fromUserId), maskId(toUserId), sent);
    }

    private void handleChannelMessage(String fromUserId, JsonNode node) throws JsonProcessingException {
        String channelId = node.path("channel").asText();
        if (!isSafeId(channelId)) {
            log.debug("频道 ID 不合法: channel={}", channelId);
            return;
        }
        if (!channelManager.isMember(channelId, fromUserId)) {
            sendError(sessionManager.getUserSessionIds(fromUserId).stream().findFirst().orElse(null),
                    "未加入频道: " + channelId);
            return;
        }
        String content = node.path("content").asText();
        String channelMsg = objectMapper.writeValueAsString(WsMessage.channel(fromUserId, channelId, content));
        Set<String> members = channelManager.getMembers(channelId);
        int sent = 0;
        for (String memberId : members) {
            if (!memberId.equals(fromUserId)) {
                sent += sessionManager.sendToUser(memberId, channelMsg);
            }
        }
        // 给发送者返回 ACK，告知消息已投递
        sessionManager.sendToUser(fromUserId,
                objectMapper.writeValueAsString(WsMessage.channelAck(channelId)));
        log.debug("频道消息: from={}, channel={}, members={}, sent={}",
                maskId(fromUserId), channelId, members.size(), sent);
    }

    private void handleJoinChannel(String userId, JsonNode node) throws JsonProcessingException {
        String channelId = node.path("channel").asText();
        if (!isSafeId(channelId)) return;

        channelManager.join(channelId, userId);
        String joinMsg = objectMapper.writeValueAsString(WsMessage.join(userId, channelId));
        for (String memberId : channelManager.getMembers(channelId)) {
            if (!memberId.equals(userId)) {
                sessionManager.sendToUser(memberId, joinMsg);
            }
        }
    }

    private void handleLeaveChannel(String userId, JsonNode node) throws JsonProcessingException {
        String channelId = node.path("channel").asText();
        if (!isSafeId(channelId)) return;

        channelManager.leave(channelId, userId);
        String leaveMsg = objectMapper.writeValueAsString(WsMessage.leave(userId, channelId));
        for (String memberId : channelManager.getMembers(channelId)) {
            sessionManager.sendToUser(memberId, leaveMsg);
        }
    }

    private void handlePing(WebSocketSession session) throws JsonProcessingException {
        sessionManager.sendToSession(session.getId(),
                objectMapper.writeValueAsString(WsMessage.pong()));
    }

    /**
     * 全局广播——仅允许 session attribute {@code isAdmin=true} 的管理员发起。
     * 管理员标识由握手拦截器负责写入（需在 {@link com.basebackend.websocket.interceptor.AuthHandshakeInterceptor}
     * 中完成 JWT 解析并将角色写入 session attributes）。
     */
    private void handleBroadcast(WebSocketSession session, String fromUserId, JsonNode node)
            throws JsonProcessingException {
        Object isAdmin = session.getAttributes().get("isAdmin");
        if (!Boolean.TRUE.equals(isAdmin)) {
            log.warn("非法广播尝试: userId={}", maskId(fromUserId));
            sendError(session.getId(), "无广播权限");
            return;
        }
        String content = node.path("content").asText();
        broadcastStrategy.broadcast(
                objectMapper.writeValueAsString(WsMessage.broadcast(fromUserId, content)));
        log.info("全局广播: from={}", maskId(fromUserId));
    }

    // --- 工具方法 ---

    private String extractUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        if (userId != null) return userId.toString();
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("userId=")) {
                    return param.substring(7);
                }
            }
        }
        return null;
    }

    /**
     * 校验 channelId / userId 合法性，防止注入或资源滥用
     */
    private boolean isSafeId(String id) {
        return id != null && !id.isBlank()
                && id.length() <= MAX_ID_LENGTH
                && SAFE_ID_PATTERN.matcher(id).matches();
    }

    /**
     * 序列化错误消息并发送，使用 {@link WsMessage#error} 避免手工拼接
     */
    private void sendError(String sessionId, String errorMessage) {
        if (sessionId == null) return;
        try {
            sessionManager.sendToSession(sessionId,
                    objectMapper.writeValueAsString(WsMessage.error(errorMessage)));
        } catch (JsonProcessingException e) {
            log.warn("序列化错误消息失败", e);
        }
    }

    /** 日志脱敏（委托给 SessionManager 的同名方法，保持一致） */
    private static String maskId(String id) {
        return SessionManager.maskId(id);
    }
}
