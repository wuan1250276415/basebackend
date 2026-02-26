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
