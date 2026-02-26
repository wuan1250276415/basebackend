package com.basebackend.websocket.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("WsMessage 测试")
class WsMessageTest {

    @Test
    @DisplayName("text() 创建文本消息")
    void textMessage() {
        WsMessage msg = WsMessage.text("user1", "user2", "hello");
        assertThat(msg.type()).isEqualTo(WsMessage.MessageType.TEXT);
        assertThat(msg.from()).isEqualTo("user1");
        assertThat(msg.to()).isEqualTo("user2");
        assertThat(msg.content()).isEqualTo("hello");
        assertThat(msg.id()).isNotNull().hasSize(16);
        assertThat(msg.timestamp()).isNotNull();
        assertThat(msg.metadata()).isEmpty();
    }

    @Test
    @DisplayName("system() 创建系统消息")
    void systemMessage() {
        WsMessage msg = WsMessage.system("服务器维护通知");
        assertThat(msg.type()).isEqualTo(WsMessage.MessageType.SYSTEM);
        assertThat(msg.from()).isEqualTo("system");
        assertThat(msg.to()).isNull();
        assertThat(msg.content()).isEqualTo("服务器维护通知");
    }

    @Test
    @DisplayName("event() 创建事件消息")
    void eventMessage() {
        WsMessage msg = WsMessage.event("user_joined", java.util.Map.of("userId", "u1"));
        assertThat(msg.type()).isEqualTo(WsMessage.MessageType.EVENT);
        assertThat(msg.content()).isEqualTo("user_joined");
        assertThat(msg.metadata()).containsEntry("userId", "u1");
    }

    @Test
    @DisplayName("heartbeat() 创建心跳消息")
    void heartbeatMessage() {
        WsMessage msg = WsMessage.heartbeat();
        assertThat(msg.type()).isEqualTo(WsMessage.MessageType.HEARTBEAT);
        assertThat(msg.content()).isEqualTo("ping");
    }

    @Test
    @DisplayName("error() 创建错误消息")
    void errorMessage() {
        WsMessage msg = WsMessage.error("连接超时");
        assertThat(msg.type()).isEqualTo(WsMessage.MessageType.ERROR);
        assertThat(msg.content()).isEqualTo("连接超时");
    }

    @Test
    @DisplayName("每次生成的 ID 不同")
    void uniqueIds() {
        WsMessage msg1 = WsMessage.system("a");
        WsMessage msg2 = WsMessage.system("b");
        assertThat(msg1.id()).isNotEqualTo(msg2.id());
    }

    @Test
    @DisplayName("MessageType 枚举完整")
    void messageTypes() {
        assertThat(WsMessage.MessageType.values()).containsExactlyInAnyOrder(
                WsMessage.MessageType.TEXT,
                WsMessage.MessageType.SYSTEM,
                WsMessage.MessageType.HEARTBEAT,
                WsMessage.MessageType.EVENT,
                WsMessage.MessageType.JOIN,
                WsMessage.MessageType.LEAVE,
                WsMessage.MessageType.ERROR
        );
    }
}
