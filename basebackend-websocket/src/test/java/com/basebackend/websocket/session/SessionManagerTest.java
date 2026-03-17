package com.basebackend.websocket.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("SessionManager 测试")
class SessionManagerTest {

    private SessionManager manager;

    @BeforeEach
    void setUp() {
        // 默认配置：最大 10 个全局连接，单用户 3 个
        manager = new SessionManager(10, 3, 5_000, 512);
    }

    private WebSocketSession mockSession(String id) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        when(session.getUri()).thenReturn(new URI("/ws"));
        return session;
    }

    @Test
    @DisplayName("注册后用户在线")
    void registerAndOnline() throws Exception {
        WebSocketSession s = mockSession("s1");
        manager.register(s, "user1");

        assertThat(manager.isOnline("user1")).isTrue();
        assertThat(manager.getConnectionCount()).isEqualTo(1);
        assertThat(manager.getOnlineUserCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("注销后用户离线")
    void unregisterGoesOffline() throws Exception {
        WebSocketSession s = mockSession("s1");
        manager.register(s, "user1");
        manager.unregister("s1");

        assertThat(manager.isOnline("user1")).isFalse();
        assertThat(manager.getConnectionCount()).isZero();
    }

    @Test
    @DisplayName("同一用户多设备连接")
    void multiDeviceConnection() throws Exception {
        manager.register(mockSession("s1"), "user1");
        manager.register(mockSession("s2"), "user1");

        assertThat(manager.getUserSessionIds("user1")).containsExactlyInAnyOrder("s1", "s2");
        assertThat(manager.getOnlineUserCount()).isEqualTo(1);
        assertThat(manager.getConnectionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("sendToUser 向在线用户发送消息")
    void sendToOnlineUser() throws Exception {
        WebSocketSession s = mockSession("s1");
        manager.register(s, "user1");

        int sent = manager.sendToUser("user1", "{\"type\":\"test\"}");

        assertThat(sent).isEqualTo(1);
        verify(s).sendMessage(any(WebSocketMessage.class));
    }

    @Test
    @DisplayName("sendToUser 用户离线返回 0")
    void sendToOfflineUserReturnsZero() {
        int sent = manager.sendToUser("nobody", "msg");
        assertThat(sent).isZero();
    }

    @Test
    @DisplayName("broadcast 发送给所有在线会话")
    void broadcastToAll() throws Exception {
        WebSocketSession s1 = mockSession("s1");
        WebSocketSession s2 = mockSession("s2");
        manager.register(s1, "user1");
        manager.register(s2, "user2");

        int sent = manager.broadcast("ping");

        assertThat(sent).isEqualTo(2);
        verify(s1).sendMessage(any(WebSocketMessage.class));
        verify(s2).sendMessage(any(WebSocketMessage.class));
    }

    @Test
    @DisplayName("getOnlineUserIds 返回不可变集合")
    void onlineUserIdsImmutable() throws Exception {
        manager.register(mockSession("s1"), "user1");

        assertThatThrownBy(() -> manager.getOnlineUserIds().add("hacker"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("disconnectUser 关闭用户所有连接")
    void disconnectUser() throws Exception {
        WebSocketSession s1 = mockSession("s1");
        WebSocketSession s2 = mockSession("s2");
        manager.register(s1, "user1");
        manager.register(s2, "user1");

        manager.disconnectUser("user1");

        assertThat(manager.isOnline("user1")).isFalse();
        assertThat(manager.getConnectionCount()).isZero();
        // 两个 session 都应被关闭
        verify(s1).close(any());
        verify(s2).close(any());
    }

    @Test
    @DisplayName("超全局连接数限制时拒绝新连接")
    void globalConnectionLimitRejectsNew() throws Exception {
        // 填满 10 个连接
        for (int i = 0; i < 10; i++) {
            manager.register(mockSession("s" + i), "user" + i);
        }
        assertThat(manager.getConnectionCount()).isEqualTo(10);

        // 第 11 个连接被拒绝
        WebSocketSession extra = mockSession("s-extra");
        boolean registered = manager.register(extra, "userExtra");

        assertThat(registered).isFalse();
        assertThat(manager.getConnectionCount()).isEqualTo(10);
        verify(extra).close(any()); // 应调用 close
    }

    @Test
    @DisplayName("超单用户连接数限制时拒绝新连接")
    void userConnectionLimitRejectsNew() throws Exception {
        manager.register(mockSession("s1"), "user1");
        manager.register(mockSession("s2"), "user1");
        manager.register(mockSession("s3"), "user1");

        // 第 4 个连接被拒绝（限制为 3）
        WebSocketSession s4 = mockSession("s4");
        boolean registered = manager.register(s4, "user1");

        assertThat(registered).isFalse();
        assertThat(manager.getUserSessionIds("user1")).hasSize(3);
        verify(s4).close(any());
    }

    @Test
    @DisplayName("updateLastActive 更新活跃时间")
    void updateLastActive() throws Exception {
        manager.register(mockSession("s1"), "user1");

        // 刚注册，不应超时
        List<String> stale = manager.getStaleSessionIds(Duration.ofSeconds(60));
        assertThat(stale).doesNotContain("s1");
    }

    @Test
    @DisplayName("getStaleSessionIds 返回超时会话")
    void getStaleSessionIds() throws Exception {
        manager.register(mockSession("s1"), "user1");

        // 使用极短超时（负数偏移），让 s1 立即变成超时
        List<String> stale = manager.getStaleSessionIds(Duration.ofMillis(-1));
        assertThat(stale).contains("s1");
    }

    @Test
    @DisplayName("disconnectSession 移除超时会话")
    void disconnectSession() throws Exception {
        WebSocketSession s = mockSession("s1");
        manager.register(s, "user1");
        manager.disconnectSession("s1");

        assertThat(manager.getSession("s1")).isNull();
        assertThat(manager.isOnline("user1")).isFalse();
    }

    @Test
    @DisplayName("sendToSession 会话已关闭时返回 false")
    void sendToClosedSession() throws Exception {
        WebSocketSession s = mockSession("s1");
        when(s.isOpen()).thenReturn(false);
        manager.register(s, "user1");

        boolean sent = manager.sendToSession("s1", "msg");
        assertThat(sent).isFalse();
    }

    @Test
    @DisplayName("sendToSession 不存在的会话返回 false")
    void sendToNonexistentSession() {
        boolean sent = manager.sendToSession("ghost", "msg");
        assertThat(sent).isFalse();
    }

    @Test
    @DisplayName("getSession 返回会话信息")
    void getSession() throws Exception {
        manager.register(mockSession("s1"), "user1");
        SessionManager.SessionInfo info = manager.getSession("s1");

        assertThat(info).isNotNull();
        assertThat(info.userId()).isEqualTo("user1");
        assertThat(info.connectedAt()).isNotNull();
    }

    @Test
    @DisplayName("unregister 不存在的会话不报错")
    void unregisterNonexistent() {
        assertThatCode(() -> manager.unregister("ghost")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendToUser IO 异常时返回 0 且不抛出")
    void sendToUserIoException() throws Exception {
        WebSocketSession s = mockSession("s1");
        doThrow(new IOException("connection reset")).when(s).sendMessage(any());
        manager.register(s, "user1");

        assertThatCode(() -> {
            int sent = manager.sendToUser("user1", "msg");
            assertThat(sent).isZero();
        }).doesNotThrowAnyException();
    }
}
