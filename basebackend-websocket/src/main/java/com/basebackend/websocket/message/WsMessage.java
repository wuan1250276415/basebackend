package com.basebackend.websocket.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.time.Instant;
import java.util.Map;

/**
 * WebSocket 消息（wire-format 模型）
 *
 * <p>该 record 同时作为领域模型和序列化载体：
 * <ul>
 *   <li>{@code type} 序列化为小写字符串（TEXT → "text"，CHANNEL_ACK → "channel-ack"）</li>
 *   <li>{@code timestamp} 序列化为 epoch 毫秒（long），与 JSON 客户端兼容</li>
 *   <li>null 字段自动省略（{@link JsonInclude#NON_NULL}）</li>
 *   <li>空 metadata 自动省略（{@link JsonInclude#NON_EMPTY}）</li>
 * </ul>
 *
 * <p>消息 {@code to} 字段语义：
 * <ul>
 *   <li>私聊（TEXT）：目标 userId</li>
 *   <li>频道（CHANNEL / JOIN / LEAVE / CHANNEL_ACK）：channelId</li>
 *   <li>广播（BROADCAST / SYSTEM / PONG / ERROR）：null</li>
 * </ul>
 *
 * @param id        消息 ID（16位 hex）
 * @param type      消息类型
 * @param from      发送者 ID
 * @param to        目标（userId / channelId / null=广播）
 * @param content   消息内容
 * @param metadata  附加元数据（仅 EVENT 类型有值）
 * @param timestamp 时间戳（内部使用 Instant，序列化为 epoch ms）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WsMessage(
        String id,
        MessageType type,
        String from,
        String to,
        String content,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, Object> metadata,
        @JsonIgnore Instant timestamp
) {
    /**
     * wire-format 时间戳：epoch 毫秒
     * <p>
     * 通过独立方法输出，避免 Jackson 默认将 Instant 序列化为 ISO-8601 字符串。
     */
    @JsonProperty("timestamp")
    public long timestampMillis() {
        return timestamp.toEpochMilli();
    }

    /**
     * 消息类型枚举
     * <p>序列化为小写字符串（下划线转连字符），例如：
     * {@code TEXT} → {@code "text"}，{@code CHANNEL_ACK} → {@code "channel-ack"}
     */
    public enum MessageType {
        /** 私聊消息 */
        TEXT,
        /** 系统通知（如服务器维护公告） */
        SYSTEM,
        /** 服务端主动 ping */
        HEARTBEAT,
        /** 响应客户端 ping 的 pong */
        PONG,
        /** 业务事件（携带 metadata） */
        EVENT,
        /** 频道消息 */
        CHANNEL,
        /** 频道消息发送确认（ACK） */
        CHANNEL_ACK,
        /** 用户加入频道通知 */
        JOIN,
        /** 用户离开频道通知 */
        LEAVE,
        /** 全局广播 */
        BROADCAST,
        /** 错误消息 */
        ERROR;

        @JsonValue
        public String toJsonValue() {
            return name().toLowerCase().replace('_', '-');
        }
    }

    // --- 工厂方法 ---

    /** 私聊消息（to = 目标 userId） */
    public static WsMessage text(String from, String to, String content) {
        return new WsMessage(generateId(), MessageType.TEXT, from, to, content, Map.of(), Instant.now());
    }

    /** 频道消息（to = channelId） */
    public static WsMessage channel(String from, String channelId, String content) {
        return new WsMessage(generateId(), MessageType.CHANNEL, from, channelId, content, Map.of(), Instant.now());
    }

    /** 频道消息发送确认（to = channelId） */
    public static WsMessage channelAck(String channelId) {
        return new WsMessage(generateId(), MessageType.CHANNEL_ACK, "server", channelId, "sent", Map.of(), Instant.now());
    }

    /** 用户加入频道通知（from = userId, to = channelId） */
    public static WsMessage join(String userId, String channelId) {
        return new WsMessage(generateId(), MessageType.JOIN, userId, channelId, null, Map.of(), Instant.now());
    }

    /** 用户离开频道通知（from = userId, to = channelId） */
    public static WsMessage leave(String userId, String channelId) {
        return new WsMessage(generateId(), MessageType.LEAVE, userId, channelId, null, Map.of(), Instant.now());
    }

    /** 服务端主动心跳 ping（全量发送给所有在线会话） */
    public static WsMessage heartbeat() {
        return new WsMessage(generateId(), MessageType.HEARTBEAT, "server", null, "ping", Map.of(), Instant.now());
    }

    /** 响应客户端 ping 的 pong */
    public static WsMessage pong() {
        return new WsMessage(generateId(), MessageType.PONG, "server", null, null, Map.of(), Instant.now());
    }

    /** 全局广播消息 */
    public static WsMessage broadcast(String from, String content) {
        return new WsMessage(generateId(), MessageType.BROADCAST, from, null, content, Map.of(), Instant.now());
    }

    /** 系统通知（无发送者） */
    public static WsMessage system(String content) {
        return new WsMessage(generateId(), MessageType.SYSTEM, "system", null, content, Map.of(), Instant.now());
    }

    /** 业务事件消息（携带结构化 metadata） */
    public static WsMessage event(String eventName, Map<String, Object> data) {
        return new WsMessage(generateId(), MessageType.EVENT, "system", null, eventName, data, Instant.now());
    }

    /** 错误消息 */
    public static WsMessage error(String errorMsg) {
        return new WsMessage(generateId(), MessageType.ERROR, "system", null, errorMsg, Map.of(), Instant.now());
    }

    private static String generateId() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
