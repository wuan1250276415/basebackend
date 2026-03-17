package com.basebackend.websocket.handler;

import com.basebackend.websocket.broadcast.BroadcastStrategy;
import com.basebackend.websocket.channel.ChannelManager;
import com.basebackend.websocket.session.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("WsMessageHandler 测试")
class WsMessageHandlerTest {

    private SessionManager sessionManager;
    private ChannelManager channelManager;
    private BroadcastStrategy broadcastStrategy;
    private ObjectMapper objectMapper;
    private WsMessageHandler handler;

    private static final int MAX_MSG_BYTES = 64 * 1024;

    @BeforeEach
    void setUp() {
        sessionManager = mock(SessionManager.class);
        channelManager = mock(ChannelManager.class);
        broadcastStrategy = mock(BroadcastStrategy.class);
        objectMapper = new ObjectMapper();
        handler = new WsMessageHandler(sessionManager, channelManager, objectMapper,
                MAX_MSG_BYTES, broadcastStrategy);
    }

    private WebSocketSession mockSession(String sessionId, String userId) throws Exception {
        return mockSession(sessionId, userId, false);
    }

    private WebSocketSession mockSession(String sessionId, String userId, boolean isAdmin) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(sessionId);
        when(session.isOpen()).thenReturn(true);
        when(session.getUri()).thenReturn(new URI("/ws"));

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", userId);
        if (isAdmin) attrs.put("isAdmin", true);
        when(session.getAttributes()).thenReturn(attrs);

        SessionManager.SessionInfo info = mock(SessionManager.SessionInfo.class);
        when(info.userId()).thenReturn(userId);
        when(info.session()).thenReturn(session);
        when(sessionManager.getSession(sessionId)).thenReturn(info);

        return session;
    }

    // --- 连接建立 ---

    @Test
    @DisplayName("连接建立时注册会话")
    void connectionEstablishedRegistersSession() throws Exception {
        WebSocketSession session = mockSession("s1", "user1");
        handler.afterConnectionEstablished(session);
        verify(sessionManager).register(session, "user1");
    }

    @Test
    @DisplayName("匿名连接使用 anonymous- 前缀")
    void anonymousConnectionFallback() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("abc123");
        when(session.getUri()).thenReturn(new URI("/ws"));
        when(session.getAttributes()).thenReturn(new HashMap<>());

        handler.afterConnectionEstablished(session);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sessionManager).register(eq(session), captor.capture());
        assertThat(captor.getValue()).startsWith("anonymous-");
    }

    // --- ping/pong ---

    @Test
    @DisplayName("ping 消息返回 pong（type=pong）")
    void pingReturnsPong() throws Exception {
        WebSocketSession session = mockSession("s1", "user1");
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"ping\"}"));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sessionManager).sendToSession(eq("s1"), captor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(captor.getValue(), Map.class);
        assertThat(response.get("type")).isEqualTo("pong");
        assertThat(response).containsKey("timestamp");
        // 序列化后不含 content（null 字段已省略）
        assertThat(response).doesNotContainKey("content");
    }

    // --- 私聊 ---

    @Test
    @DisplayName("text 消息路由到目标用户")
    void textMessageRoutedToTarget() throws Exception {
        WebSocketSession session = mockSession("s1", "user1");
        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"text\",\"to\":\"user2\",\"content\":\"hello\"}"));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sessionManager).sendToUser(eq("user2"), captor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> msg = objectMapper.readValue(captor.getValue(), Map.class);
        assertThat(msg.get("type")).isEqualTo("text");
        assertThat(msg.get("from")).isEqualTo("user1");
        assertThat(msg.get("to")).isEqualTo("user2");
        assertThat(msg.get("content")).isEqualTo("hello");
        assertThat(msg).containsKey("id");
        assertThat(msg).containsKey("timestamp");
    }

    @Test
    @DisplayName("text 消息内容含特殊字符时正确序列化（防 JSON 注入）")
    void textMessageEscapesSpecialChars() throws Exception {
        WebSocketSession session = mockSession("s1", "user1");
        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"text\",\"to\":\"user2\",\"content\":\"\\\"injected\\\"\"}"));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sessionManager).sendToUser(eq("user2"), captor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> msg = objectMapper.readValue(captor.getValue(), Map.class);
        assertThat(msg.get("content")).isEqualTo("\"injected\"");
    }

    @Test
    @DisplayName("私聊目标 userId 含注入字符时忽略")
    void textMessageInvalidToIgnored() throws Exception {
        WebSocketSession session = mockSession("s1", "user1");
        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"text\",\"to\":\"../../etc\",\"content\":\"x\"}"));
        verify(sessionManager, never()).sendToUser(anyString(), anyString());
    }

    // --- 频道消息 ---

    @Test
    @DisplayName("频道消息：非成员发送返回 error")
    void channelMessageRejectedIfNotMember() throws Exception {
        WebSocketSession session = mockSession("s1", "user1");
        when(channelManager.isMember("room1", "user1")).thenReturn(false);
        when(sessionManager.getUserSessionIds("user1")).thenReturn(Set.of("s1"));

        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"channel\",\"channel\":\"room1\",\"content\":\"hi\"}"));

        verify(sessionManager, never()).sendToUser(eq("room1"), anyString());
        verify(sessionManager).sendToSession(eq("s1"), argThat(s -> s.contains("\"error\"")));
    }

    @Test
    @DisplayName("频道消息：发给其他成员并向发送者返回 ACK")
    void channelMessageDeliveredWithAck() throws Exception {
        WebSocketSession session = mockSession("s1", "user1");
        when(channelManager.isMember("room1", "user1")).thenReturn(true);
        when(channelManager.getMembers("room1")).thenReturn(Set.of("user1", "user2", "user3"));

        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"channel\",\"channel\":\"room1\",\"content\":\"hi\"}"));

        // user2 / user3 收到频道消息
        ArgumentCaptor<String> cap2 = ArgumentCaptor.forClass(String.class);
        verify(sessionManager).sendToUser(eq("user2"), cap2.capture());
        assertThat(cap2.getValue()).contains("\"channel\"");

        verify(sessionManager).sendToUser(eq("user3"), anyString());
        // 发送者收到 channel-ack
        verify(sessionManager).sendToUser(eq("user1"), argThat(s -> s.contains("channel-ack")));
    }

    // --- join / leave ---

    @Test
    @DisplayName("join 通知频道其他成员（发送者不收）")
    void joinNotifiesOtherMembers() throws Exception {
        WebSocketSession session = mockSession("s1", "user1");
        when(channelManager.getMembers("room1")).thenReturn(Set.of("user1", "user2"));

        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"join\",\"channel\":\"room1\"}"));

        verify(channelManager).join("room1", "user1");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sessionManager).sendToUser(eq("user2"), captor.capture());
        assertThat(captor.getValue()).contains("\"join\"");
        verify(sessionManager, never()).sendToUser(eq("user1"), argThat(s -> s.contains("\"join\"")));
    }

    @Test
    @DisplayName("leave 通知频道剩余成员")
    void leaveNotifiesRemainingMembers() throws Exception {
        WebSocketSession session = mockSession("s1", "user1");
        when(channelManager.getMembers("room1")).thenReturn(Set.of("user2"));

        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"leave\",\"channel\":\"room1\"}"));

        verify(channelManager).leave("room1", "user1");
        verify(sessionManager).sendToUser(eq("user2"), argThat(s -> s.contains("\"leave\"")));
    }

    // --- 广播 ---

    @Test
    @DisplayName("broadcast 无 isAdmin 标志时被拒绝")
    void broadcastDeniedWithoutAdminFlag() throws Exception {
        WebSocketSession session = mockSession("s1", "user1", false);

        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"broadcast\",\"content\":\"hello\"}"));

        verify(broadcastStrategy, never()).broadcast(anyString());
        verify(sessionManager).sendToSession(eq("s1"), argThat(s -> s.contains("\"error\"")));
    }

    @Test
    @DisplayName("broadcast 管理员可以广播（通过 BroadcastStrategy）")
    void broadcastAllowedForAdmin() throws Exception {
        WebSocketSession session = mockSession("s1", "admin1", true);

        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"broadcast\",\"content\":\"system notice\"}"));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(broadcastStrategy).broadcast(captor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> msg = objectMapper.readValue(captor.getValue(), Map.class);
        assertThat(msg.get("type")).isEqualTo("broadcast");
        assertThat(msg.get("from")).isEqualTo("admin1");
        assertThat(msg.get("content")).isEqualTo("system notice");
    }

    // --- 消息大小限制 ---

    @Test
    @DisplayName("超大消息被拒绝并返回 error")
    void oversizedMessageRejected() throws Exception {
        WsMessageHandler tinyHandler =
                new WsMessageHandler(sessionManager, channelManager, objectMapper, 10, broadcastStrategy);
        WebSocketSession session = mockSession("s1", "user1");
        String big = "{\"type\":\"text\",\"to\":\"u2\",\"content\":\"" + "x".repeat(100) + "\"}";

        tinyHandler.handleTextMessage(session, new TextMessage(big));

        verify(sessionManager, never()).sendToUser(anyString(), anyString());
        verify(sessionManager).sendToSession(eq("s1"), argThat(s -> s.contains("\"error\"")));
    }

    // --- 非法 ID 校验 ---

    @Test
    @DisplayName("join 非法 channelId 被忽略（路径穿越字符）")
    void invalidChannelIdIgnored() throws Exception {
        WebSocketSession session = mockSession("s1", "user1");
        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"join\",\"channel\":\"../../etc/passwd\"}"));
        verify(channelManager, never()).join(anyString(), anyString());
    }

    @Test
    @DisplayName("join 超长 channelId 被忽略")
    void tooLongChannelIdIgnored() throws Exception {
        WebSocketSession session = mockSession("s1", "user1");
        String longId = "a".repeat(200);
        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"join\",\"channel\":\"" + longId + "\"}"));
        verify(channelManager, never()).join(anyString(), anyString());
    }

    // --- 错误处理 ---

    @Test
    @DisplayName("非法 JSON 触发 error 响应")
    void invalidJsonReturnsError() throws Exception {
        WebSocketSession session = mockSession("s1", "user1");
        handler.handleTextMessage(session, new TextMessage("not-json"));
        verify(sessionManager).sendToSession(eq("s1"), argThat(s -> s.contains("\"error\"")));
    }

    @Test
    @DisplayName("error 响应不泄露内部异常详情")
    void errorResponseDoesNotLeakInternals() throws Exception {
        WebSocketSession session = mockSession("s1", "user1");
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"text\""));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sessionManager).sendToSession(eq("s1"), captor.capture());

        // 不含异常类名或堆栈信息
        assertThat(captor.getValue()).doesNotContain("Exception");
        assertThat(captor.getValue()).doesNotContain("at com.");
        // 内容是固定的用户友好提示，不包含 e.getMessage()
        assertThat(captor.getValue()).contains("消息格式错误");
    }

    // --- 连接关闭 ---

    @Test
    @DisplayName("连接关闭时离开所有频道并注销")
    void connectionClosedLeaveAllAndUnregister() throws Exception {
        WebSocketSession session = mockSession("s1", "user1");
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
        verify(channelManager).leaveAll("user1");
        verify(sessionManager).unregister("s1");
    }

    @Test
    @DisplayName("传输错误时也正确清理状态")
    void transportErrorCleansUp() throws Exception {
        WebSocketSession session = mockSession("s1", "user1");
        handler.handleTransportError(session, new RuntimeException("timeout"));
        verify(channelManager).leaveAll("user1");
        verify(sessionManager).unregister("s1");
    }

    // --- updateLastActive ---

    @Test
    @DisplayName("处理每条消息后调用 updateLastActive")
    void handleMessageUpdatesLastActive() throws Exception {
        WebSocketSession session = mockSession("s1", "user1");
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"ping\"}"));
        verify(sessionManager).updateLastActive("s1");
    }
}
