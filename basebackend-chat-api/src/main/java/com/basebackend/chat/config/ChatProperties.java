package com.basebackend.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天模块配置属性
 */
@Data
@ConfigurationProperties(prefix = "basebackend.chat")
public class ChatProperties {

    /** 是否启用聊天模块 */
    private boolean enabled = false;

    /** WebSocket 端点路径 */
    private String wsEndpoint = "/ws/chat";

    /** WebSocket 允许的跨域来源 */
    private List<String> allowedOrigins = new ArrayList<>(List.of("*"));

    /** 消息撤回时限（秒），默认2分钟 */
    private int revokeTimeoutSeconds = 120;

    /** 历史消息默认每页数量 */
    private int defaultMessagePageSize = 30;

    /** 历史消息最大每页数量 */
    private int maxMessagePageSize = 100;

    /** 群最大成员数 */
    private int defaultMaxGroupMembers = 500;

    /** 心跳超时阈值（毫秒），默认90秒 — 超过此时间未收到心跳则关闭连接 */
    private long heartbeatTimeoutMs = 90_000;
}
