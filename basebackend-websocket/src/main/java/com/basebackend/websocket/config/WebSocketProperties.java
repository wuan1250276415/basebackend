package com.basebackend.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket 配置属性
 *
 * <pre>
 * basebackend:
 *   websocket:
 *     enabled: true
 *     endpoint: /ws
 *     allowed-origins:
 *       - "*"
 *     max-connections: 10000        # 全局最大连接数
 *     max-user-connections: 5       # 单用户最大连接数
 *     max-message-size-kb: 64       # 单条消息最大字节数(KB)
 *     send-time-limit-ms: 5000      # 单次发送超时(ms)，超时关闭连接
 *     send-buffer-size-kb: 512      # 发送缓冲区(KB)，超出关闭连接
 *     heartbeat:
 *       interval: 25s
 *       timeout: 60s
 *     broadcast:
 *       type: memory          # memory | redis
 *       redis-topic: ws:broadcast
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "basebackend.websocket")
public class WebSocketProperties {

    /** 是否启用 WebSocket */
    private boolean enabled = false;

    /** WebSocket 端点路径 */
    private String endpoint = "/ws";

    /** 允许的源 */
    private List<String> allowedOrigins = new ArrayList<>(List.of("*"));

    /** 是否启用 SockJS 回退 */
    private boolean sockjsEnabled = true;

    /** 全局最大连接数，超出时拒绝新连接 */
    private int maxConnections = 10_000;

    /** 单用户最大连接数（多设备），超出时拒绝 */
    private int maxUserConnections = 5;

    /** 单条消息最大大小 (KB)，超出时丢弃并返回错误 */
    private int maxMessageSizeKb = 64;

    /** 单次发送超时 (ms)，超时强制关闭连接 */
    private int sendTimeLimitMs = 5_000;

    /** 发送缓冲区大小 (KB)，积压超出后强制关闭连接 */
    private int sendBufferSizeKb = 512;

    /** 心跳配置 */
    private HeartbeatConfig heartbeat = new HeartbeatConfig();

    /** 集群广播配置 */
    private BroadcastConfig broadcast = new BroadcastConfig();

    @Data
    public static class HeartbeatConfig {
        /** 心跳发送间隔 */
        private Duration interval = Duration.ofSeconds(25);
        /** 心跳超时时间（超时视为断线） */
        private Duration timeout = Duration.ofSeconds(60);
    }

    @Data
    public static class BroadcastConfig {
        /** 广播类型：memory / redis */
        private String type = "memory";
        /** Redis 广播 Topic */
        private String redisTopic = "ws:broadcast";
    }
}
