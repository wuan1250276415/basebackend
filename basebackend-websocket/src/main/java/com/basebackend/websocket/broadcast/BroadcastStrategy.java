package com.basebackend.websocket.broadcast;

/**
 * WebSocket 广播策略
 * <p>
 * 抽象广播实现细节，支持以下模式：
 * <ul>
 *   <li>{@code memory}：仅广播到当前节点的在线会话（单机模式）</li>
 *   <li>{@code redis}：通过 Redis Pub/Sub 广播到集群中所有节点（集群模式）</li>
 * </ul>
 * 通过 {@code basebackend.websocket.broadcast.type} 配置切换。
 */
public interface BroadcastStrategy {

    /**
     * 广播消息
     *
     * @param message 已序列化的 JSON 字符串
     */
    void broadcast(String message);
}
