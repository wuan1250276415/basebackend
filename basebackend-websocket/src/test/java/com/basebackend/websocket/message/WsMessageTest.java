package com.basebackend.websocket.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("WsMessage 测试")
class WsMessageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("text() 创建私聊消息")
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
    @DisplayName("channel() 创建频道消息（to = channelId）")
    void channelMessage() {
        WsMessage msg = WsMessage.channel("user1", "room1", "hi");
        assertThat(msg.type()).isEqualTo(WsMessage.MessageType.CHANNEL);
        assertThat(msg.from()).isEqualTo("user1");
        assertThat(msg.to()).isEqualTo("room1");
        assertThat(msg.content()).isEqualTo("hi");
    }

    @Test
    @DisplayName("channelAck() 创建频道 ACK")
    void channelAckMessage() {
        WsMessage msg = WsMessage.channelAck("room1");
        assertThat(msg.type()).isEqualTo(WsMessage.MessageType.CHANNEL_ACK);
        assertThat(msg.from()).isEqualTo("server");
        assertThat(msg.to()).isEqualTo("room1");
        assertThat(msg.content()).isEqualTo("sent");
    }

    @Test
    @DisplayName("join() 加入频道通知")
    void joinMessage() {
        WsMessage msg = WsMessage.join("user1", "room1");
        assertThat(msg.type()).isEqualTo(WsMessage.MessageType.JOIN);
        assertThat(msg.from()).isEqualTo("user1");
        assertThat(msg.to()).isEqualTo("room1");
        assertThat(msg.content()).isNull();
    }

    @Test
    @DisplayName("leave() 离开频道通知")
    void leaveMessage() {
        WsMessage msg = WsMessage.leave("user1", "room1");
        assertThat(msg.type()).isEqualTo(WsMessage.MessageType.LEAVE);
        assertThat(msg.from()).isEqualTo("user1");
        assertThat(msg.to()).isEqualTo("room1");
    }

    @Test
    @DisplayName("pong() 响应客户端 ping")
    void pongMessage() {
        WsMessage msg = WsMessage.pong();
        assertThat(msg.type()).isEqualTo(WsMessage.MessageType.PONG);
        assertThat(msg.from()).isEqualTo("server");
        assertThat(msg.content()).isNull();
    }

    @Test
    @DisplayName("broadcast() 创建广播消息")
    void broadcastMessage() {
        WsMessage msg = WsMessage.broadcast("admin1", "notice");
        assertThat(msg.type()).isEqualTo(WsMessage.MessageType.BROADCAST);
        assertThat(msg.from()).isEqualTo("admin1");
        assertThat(msg.to()).isNull();
        assertThat(msg.content()).isEqualTo("notice");
    }

    @Test
    @DisplayName("system() 创建系统通知")
    void systemMessage() {
        WsMessage msg = WsMessage.system("服务器维护通知");
        assertThat(msg.type()).isEqualTo(WsMessage.MessageType.SYSTEM);
        assertThat(msg.from()).isEqualTo("system");
        assertThat(msg.to()).isNull();
        assertThat(msg.content()).isEqualTo("服务器维护通知");
    }

    @Test
    @DisplayName("event() 创建事件消息（携带 metadata）")
    void eventMessage() {
        WsMessage msg = WsMessage.event("user_joined", Map.of("userId", "u1"));
        assertThat(msg.type()).isEqualTo(WsMessage.MessageType.EVENT);
        assertThat(msg.content()).isEqualTo("user_joined");
        assertThat(msg.metadata()).containsEntry("userId", "u1");
    }

    @Test
    @DisplayName("heartbeat() 服务端主动 ping")
    void heartbeatMessage() {
        WsMessage msg = WsMessage.heartbeat();
        assertThat(msg.type()).isEqualTo(WsMessage.MessageType.HEARTBEAT);
        assertThat(msg.from()).isEqualTo("server");
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
    @DisplayName("MessageType 枚举完整（含新增类型）")
    void messageTypesComplete() {
        assertThat(WsMessage.MessageType.values()).containsExactlyInAnyOrder(
                WsMessage.MessageType.TEXT,
                WsMessage.MessageType.SYSTEM,
                WsMessage.MessageType.HEARTBEAT,
                WsMessage.MessageType.PONG,
                WsMessage.MessageType.EVENT,
                WsMessage.MessageType.CHANNEL,
                WsMessage.MessageType.CHANNEL_ACK,
                WsMessage.MessageType.JOIN,
                WsMessage.MessageType.LEAVE,
                WsMessage.MessageType.BROADCAST,
                WsMessage.MessageType.ERROR
        );
    }

    // --- 序列化验证 ---

    @Test
    @DisplayName("type 序列化为小写字符串")
    void typeSerializesToLowercase() throws Exception {
        WsMessage msg = WsMessage.text("u1", "u2", "hello");
        JsonNode json = objectMapper.valueToTree(msg);
        assertThat(json.get("type").asText()).isEqualTo("text");
    }

    @Test
    @DisplayName("CHANNEL_ACK 序列化为 channel-ack（下划线转连字符）")
    void channelAckSerializesWithHyphen() throws Exception {
        WsMessage msg = WsMessage.channelAck("room1");
        JsonNode json = objectMapper.valueToTree(msg);
        assertThat(json.get("type").asText()).isEqualTo("channel-ack");
    }

    @Test
    @DisplayName("timestamp 序列化为 epoch 毫秒（long）")
    void timestampSerializesAsEpochMillis() throws Exception {
        long before = System.currentTimeMillis();
        WsMessage msg = WsMessage.system("test");
        long after = System.currentTimeMillis();

        JsonNode json = objectMapper.valueToTree(msg);
        long ts = json.get("timestamp").asLong();

        assertThat(ts).isBetween(before, after);
    }

    @Test
    @DisplayName("null 字段不出现在序列化结果中")
    void nullFieldsExcluded() throws Exception {
        WsMessage msg = WsMessage.pong(); // content = null, to = null
        JsonNode json = objectMapper.valueToTree(msg);
        assertThat(json.has("content")).isFalse();
        assertThat(json.has("to")).isFalse();
    }

    @Test
    @DisplayName("空 metadata 不出现在序列化结果中")
    void emptyMetadataExcluded() throws Exception {
        WsMessage msg = WsMessage.text("u1", "u2", "hi"); // metadata = Map.of()
        JsonNode json = objectMapper.valueToTree(msg);
        assertThat(json.has("metadata")).isFalse();
    }

    @Test
    @DisplayName("非空 metadata 正常序列化")
    void nonEmptyMetadataIncluded() throws Exception {
        WsMessage msg = WsMessage.event("click", Map.of("x", 10, "y", 20));
        JsonNode json = objectMapper.valueToTree(msg);
        assertThat(json.has("metadata")).isTrue();
        assertThat(json.get("metadata").get("x").asInt()).isEqualTo(10);
    }
}
