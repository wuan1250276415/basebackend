package com.basebackend.websocket.broadcast;

import com.basebackend.websocket.session.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 内存广播策略（单机模式）
 * <p>
 * 直接调用 {@link SessionManager#broadcast(String)} 向本节点所有在线会话推送消息。
 * 适用于单机部署；集群场景请使用 {@link RedisBroadcastStrategy}。
 */
@Slf4j
@RequiredArgsConstructor
public class MemoryBroadcastStrategy implements BroadcastStrategy {

    private final SessionManager sessionManager;

    @Override
    public void broadcast(String message) {
        int sent = sessionManager.broadcast(message);
        log.debug("内存广播完成: sent={}", sent);
    }
}
