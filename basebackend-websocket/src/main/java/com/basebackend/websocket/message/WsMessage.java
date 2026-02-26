package com.basebackend.websocket.message;

import java.time.Instant;
import java.util.Map;

/**
 * WebSocket 消息
 *
 * @param id        消息 ID
 * @param type      消息类型（TEXT / SYSTEM / HEARTBEAT / EVENT）
 * @param from      发送者 ID
 * @param to        目标（用户ID / 频道ID / null=广播）
 * @param content   消息内容
 * @param metadata  附加元数据
 * @param timestamp 时间戳
 */
public record WsMessage(
        String id,
        MessageType type,
        String from,
        String to,
        String content,
        Map<String, Object> metadata,
        Instant timestamp
) {
    public enum MessageType {
        TEXT, SYSTEM, HEARTBEAT, EVENT, JOIN, LEAVE, ERROR
    }

    /** 创建文本消息 */
    public static WsMessage text(String from, String to, String content) {
        return new WsMessage(generateId(), MessageType.TEXT, from, to, content, Map.of(), Instant.now());
    }

    /** 创建系统消息 */
    public static WsMessage system(String content) {
        return new WsMessage(generateId(), MessageType.SYSTEM, "system", null, content, Map.of(), Instant.now());
    }

    /** 创建事件消息 */
    public static WsMessage event(String eventName, Map<String, Object> data) {
        return new WsMessage(generateId(), MessageType.EVENT, "system", null, eventName, data, Instant.now());
    }

    /** 创建心跳消息 */
    public static WsMessage heartbeat() {
        return new WsMessage(generateId(), MessageType.HEARTBEAT, "system", null, "ping", Map.of(), Instant.now());
    }

    /** 创建错误消息 */
    public static WsMessage error(String errorMsg) {
        return new WsMessage(generateId(), MessageType.ERROR, "system", null, errorMsg, Map.of(), Instant.now());
    }

    private static String generateId() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
