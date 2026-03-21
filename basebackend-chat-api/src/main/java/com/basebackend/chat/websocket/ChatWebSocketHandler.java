package com.basebackend.chat.websocket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.chat.dto.request.SendMessageRequest;
import com.basebackend.chat.entity.ChatConversationMember;
import com.basebackend.chat.entity.ChatMessage;
import com.basebackend.chat.mapper.ChatConversationMemberMapper;
import com.basebackend.chat.mapper.ChatMessageMapper;
import com.basebackend.chat.service.ChatConversationService;
import com.basebackend.chat.service.ChatMessageService;
import com.basebackend.chat.service.OnlineStatusService;
import com.basebackend.common.util.JsonUtils;
import com.basebackend.websocket.session.SessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 聊天 WebSocket 消息处理器
 * <p>
 * 复用 basebackend-websocket 模块的 {@link SessionManager} 管理连接，
 * 按 Spec 5.2 定义的帧类型路由分发业务逻辑。
 * <p>
 * 上行帧类型: chat / revoke / read / typing / ping / presence / sync
 * <br>
 * 下行帧类型: connected / chat_ack / chat / revoke / read_ack / typing / pong / presence / sync_resp / error
 */
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final SessionManager sessionManager;
    private final ChatMessageService messageService;
    private final ChatConversationService conversationService;
    private final OnlineStatusService onlineStatusService;
    private final ChatConversationMemberMapper conversationMemberMapper;
    private final ChatMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    /** sessionId -> 最后心跳时间 (epoch millis)，用于超时检测 */
    private final Map<String, Long> lastHeartbeatMap = new ConcurrentHashMap<>();

    // ======================== 生命周期 ========================

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = extractUserId(session);
        String tenantId = extractTenantId(session);

        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        // 注册会话
        sessionManager.register(session, userId, tenantId);
        lastHeartbeatMap.put(session.getId(), Instant.now().toEpochMilli());

        // 在线状态
        onlineStatusService.userOnline(Long.parseLong(tenantId), Long.parseLong(userId));

        // 发送连接成功帧
        sendFrame(session, Map.of(
                "type", "connected",
                "userId", Long.parseLong(userId),
                "serverTime", Instant.now().toEpochMilli()
        ));

        log.info("聊天WebSocket连接建立: userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String userId = extractUserId(session);
        String tenantId = extractTenantId(session);
        if (userId == null) return;

        // 刷新心跳时间
        lastHeartbeatMap.put(session.getId(), Instant.now().toEpochMilli());

        JsonNode node = objectMapper.readTree(message.getPayload());
        String type = node.has("type") ? node.get("type").asText() : "";

        switch (type) {
            case "chat" -> handleChatMessage(session, userId, tenantId, node);
            case "revoke" -> handleRevoke(session, userId, tenantId, node);
            case "read" -> handleRead(session, userId, tenantId, node);
            case "typing" -> handleTyping(userId, tenantId, node);
            case "ping" -> handlePing(session);
            case "presence" -> handlePresence(userId, tenantId, node);
            case "sync" -> handleSync(session, userId, tenantId, node);
            default -> log.debug("未知聊天消息类型: type={}, userId={}", type, userId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = extractUserId(session);
        String tenantId = extractTenantId(session);

        sessionManager.unregister(session.getId());
        lastHeartbeatMap.remove(session.getId());

        if (userId != null) {
            boolean hasOtherSessions = sessionManager.isOnline(tenantId, userId);
            onlineStatusService.userOffline(
                    Long.parseLong(tenantId),
                    Long.parseLong(userId),
                    hasOtherSessions
            );
        }

        log.info("聊天WebSocket连接关闭: userId={}, status={}", userId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("聊天WebSocket传输错误: sessionId={}, error={}", session.getId(), exception.getMessage());
    }

    // ======================== 帧处理方法 ========================

    /**
     * 处理聊天消息帧 → 持久化 + ACK + 推送
     */
    private void handleChatMessage(WebSocketSession session, String userId,
                                   String tenantId, JsonNode node) {
        try {
            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(node.get("conversationId").asLong());
            request.setType(node.get("msgType").asInt());
            request.setContent(node.has("content") ? node.get("content").asText() : null);
            request.setClientMsgId(node.has("clientMsgId") ? node.get("clientMsgId").asText() : null);

            if (node.has("quoteMessageId") && !node.get("quoteMessageId").isNull()) {
                request.setQuoteMessageId(node.get("quoteMessageId").asLong());
            }
            if (node.has("atUserIds") && !node.get("atUserIds").isNull()) {
                List<String> atList = new ArrayList<>();
                node.get("atUserIds").forEach(n -> atList.add(n.asText()));
                request.setAtUserIds(atList);
            }
            if (node.has("extra") && !node.get("extra").isNull()) {
                request.setExtra(node.get("extra").toString());
            }

            var result = messageService.sendMessage(
                    Long.parseLong(userId), Long.parseLong(tenantId), request);

            // 发送 ACK 帧
            sendFrame(session, Map.of(
                    "type", "chat_ack",
                    "clientMsgId", request.getClientMsgId() != null ? request.getClientMsgId() : "",
                    "messageId", result.get("messageId"),
                    "sendTime", result.get("sendTime"),
                    "status", result.get("status")
            ));
        } catch (Exception e) {
            log.error("处理聊天消息失败: userId={}, error={}", userId, e.getMessage(), e);
            sendError(session, "消息发送失败: " + e.getMessage());
        }
    }

    /**
     * 处理撤回帧 → 撤回 + 广播通知
     */
    private void handleRevoke(WebSocketSession session, String userId,
                              String tenantId, JsonNode node) {
        try {
            Long messageId = node.get("messageId").asLong();
            var result = messageService.revokeMessage(
                    Long.parseLong(userId), Long.parseLong(tenantId), messageId);

            // ACK 给发送方
            sendFrame(session, Map.of(
                    "type", "revoke_ack",
                    "messageId", messageId,
                    "revokeTime", result.get("revokeTime")
            ));
        } catch (Exception e) {
            log.error("处理撤回失败: userId={}, error={}", userId, e.getMessage());
            sendError(session, "撤回失败: " + e.getMessage());
        }
    }

    /**
     * 处理已读上报帧 → 更新已读状态 + 通知发送方
     */
    private void handleRead(WebSocketSession session, String userId,
                            String tenantId, JsonNode node) {
        try {
            Long conversationId = node.get("conversationId").asLong();
            Long lastReadMessageId = node.get("lastReadMessageId").asLong();

            int cleared = conversationService.markAsRead(
                    Long.parseLong(userId), Long.parseLong(tenantId),
                    conversationId, lastReadMessageId);

            // 通知会话中其他成员（发送方）该用户已读
            String readPayload = JsonUtils.toJsonString(Map.of(
                    "type", "read",
                    "conversationId", conversationId,
                    "userId", Long.parseLong(userId),
                    "lastReadMessageId", lastReadMessageId,
                    "timestamp", Instant.now().toEpochMilli()
            ));

            List<ChatConversationMember> members = conversationMemberMapper.selectList(
                    new LambdaQueryWrapper<ChatConversationMember>()
                            .eq(ChatConversationMember::getTenantId, Long.parseLong(tenantId))
                            .eq(ChatConversationMember::getConversationId, conversationId)
                            .ne(ChatConversationMember::getUserId, Long.parseLong(userId))
            );
            for (ChatConversationMember member : members) {
                sessionManager.sendToUser(tenantId, String.valueOf(member.getUserId()), readPayload);
            }

            // ACK 给自己
            sendFrame(session, Map.of(
                    "type", "read_ack",
                    "conversationId", conversationId,
                    "clearedCount", cleared
            ));
        } catch (Exception e) {
            log.error("处理已读上报失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 处理正在输入帧 → 转发给会话中其他在线用户
     */
    private void handleTyping(String userId, String tenantId, JsonNode node) {
        Long conversationId = node.get("conversationId").asLong();

        // 查询会话其他成员并推送
        String payload = JsonUtils.toJsonString(Map.of(
                "type", "typing",
                "conversationId", conversationId,
                "userId", Long.parseLong(userId),
                "timestamp", Instant.now().toEpochMilli()
        ));

        List<ChatConversationMember> members = conversationMemberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getTenantId, Long.parseLong(tenantId))
                        .eq(ChatConversationMember::getConversationId, conversationId)
                        .ne(ChatConversationMember::getUserId, Long.parseLong(userId))
        );
        for (ChatConversationMember member : members) {
            sessionManager.sendToUser(tenantId, String.valueOf(member.getUserId()), payload);
        }
    }

    /**
     * 处理心跳帧 → 回复 PONG
     */
    private void handlePing(WebSocketSession session) {
        sendFrame(session, Map.of(
                "type", "pong",
                "serverTime", Instant.now().toEpochMilli()
        ));
    }

    /**
     * 处理状态设置帧 → 更新并广播
     */
    private void handlePresence(String userId, String tenantId, JsonNode node) {
        String status = node.has("status") ? node.get("status").asText() : "online";
        onlineStatusService.setStatus(Long.parseLong(tenantId), Long.parseLong(userId), status);
    }

    /**
     * 处理重连增量同步帧 → 将每个会话的未投递消息推送给客户端
     * <p>
     * 上行帧: {@code {"type":"sync","conversations":{"50001":80095,"50002":80040}}}
     */
    private void handleSync(WebSocketSession session, String userId,
                            String tenantId, JsonNode node) {
        try {
            JsonNode convNode = node.get("conversations");
            if (convNode == null || !convNode.isObject()) return;

            Map<Long, List<Map<String, Object>>> syncResult = new LinkedHashMap<>();

            var fields = convNode.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                Long conversationId = Long.parseLong(entry.getKey());
                Long lastReceivedMsgId = entry.getValue().asLong();

                // 查询该会话中在 lastReceivedMsgId 之后的消息
                List<ChatMessage> newMessages = messageMapper.selectList(
                        new LambdaQueryWrapper<ChatMessage>()
                                .eq(ChatMessage::getTenantId, Long.parseLong(tenantId))
                                .eq(ChatMessage::getConversationId, conversationId)
                                .gt(ChatMessage::getId, lastReceivedMsgId)
                                .orderByAsc(ChatMessage::getId)
                                .last("LIMIT 200")
                );

                if (!newMessages.isEmpty()) {
                    List<Map<String, Object>> msgList = newMessages.stream()
                            .map(this::toMessageFrame)
                            .collect(Collectors.toList());
                    syncResult.put(conversationId, msgList);
                }
            }

            // 发送同步响应
            sendFrame(session, Map.of(
                    "type", "sync_resp",
                    "conversations", syncResult,
                    "serverTime", Instant.now().toEpochMilli()
            ));
        } catch (Exception e) {
            log.error("处理增量同步失败: userId={}, error={}", userId, e.getMessage(), e);
            sendError(session, "同步失败: " + e.getMessage());
        }
    }

    // ======================== 心跳超时检测 (由调度任务调用) ========================

    /**
     * 检测心跳超时的会话并关闭
     *
     * @param timeoutMs 超时阈值 (毫秒)
     */
    public void checkHeartbeatTimeout(long timeoutMs) {
        long now = Instant.now().toEpochMilli();
        List<String> expiredSessionIds = new ArrayList<>();

        lastHeartbeatMap.forEach((sessionId, lastBeat) -> {
            if (now - lastBeat > timeoutMs) {
                expiredSessionIds.add(sessionId);
            }
        });

        for (String sessionId : expiredSessionIds) {
            lastHeartbeatMap.remove(sessionId);
            var sessionInfo = sessionManager.getSession(sessionId);
            if (sessionInfo != null) {
                try {
                    log.warn("心跳超时，关闭连接: sessionId={}, userId={}",
                            sessionId, sessionInfo.userId());
                    sessionInfo.session().close(CloseStatus.SESSION_NOT_RELIABLE);
                } catch (Exception e) {
                    log.error("关闭超时会话失败: sessionId={}", sessionId, e);
                }
            }
        }
    }

    // ======================== 工具方法 ========================

    private String extractUserId(WebSocketSession session) {
        Object val = session.getAttributes().get("userId");
        return val != null ? val.toString() : null;
    }

    private String extractTenantId(WebSocketSession session) {
        Object val = session.getAttributes().get("tenantId");
        return val != null ? val.toString() : "0";
    }

    private void sendFrame(WebSocketSession session, Map<String, Object> frame) {
        try {
            session.sendMessage(new TextMessage(JsonUtils.toJsonString(frame)));
        } catch (Exception e) {
            log.error("发送帧失败: sessionId={}, type={}", session.getId(), frame.get("type"));
        }
    }

    private void sendError(WebSocketSession session, String errorMsg) {
        sendFrame(session, Map.of(
                "type", "error",
                "message", errorMsg,
                "timestamp", Instant.now().toEpochMilli()
        ));
    }

    /**
     * 将消息实体转为 Spec 5.2.2 定义的下行帧格式
     */
    private Map<String, Object> toMessageFrame(ChatMessage msg) {
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("type", "chat");
        frame.put("messageId", msg.getId());
        frame.put("conversationId", msg.getConversationId());
        frame.put("senderId", msg.getSenderId());
        frame.put("senderName", msg.getSenderName());
        frame.put("senderAvatar", msg.getSenderAvatar());
        frame.put("msgType", msg.getType());
        frame.put("content", msg.getContent());
        frame.put("extra", msg.getExtra());
        frame.put("quoteMessageId", msg.getQuoteMessageId());
        frame.put("atUserIds", msg.getAtUserIds());
        frame.put("clientMsgId", msg.getClientMsgId());
        frame.put("sendTime", msg.getSendTime());
        return frame;
    }
}
