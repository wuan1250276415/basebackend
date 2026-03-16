package com.basebackend.chat.config;

import com.basebackend.chat.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 心跳超时检测调度器
 * <p>
 * 每 30 秒扫描一次所有 WebSocket 会话，关闭超过阈值未发送心跳的连接。
 */
@Slf4j
@RequiredArgsConstructor
public class ChatHeartbeatScheduler {

    private final ChatWebSocketHandler handler;
    private final long timeoutMs;

    /**
     * 定时检测心跳超时（每30秒）
     */
    @Scheduled(fixedDelay = 30_000)
    public void checkHeartbeat() {
        handler.checkHeartbeatTimeout(timeoutMs);
    }
}
