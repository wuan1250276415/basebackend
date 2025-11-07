package com.basebackend.admin.service;

import com.alibaba.fastjson2.JSON;
import com.basebackend.admin.constants.NotificationConstants;
import com.basebackend.admin.dto.notification.NotificationMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 通知推送服务
 * 管理所有用户的 SSE 连接，并实时推送通知
 *
 * @author Claude Code
 * @since 2025-11-07
 */
@Slf4j
@Service
public class SSENotificationService {

    /**
     * 存储用户 SSE 连接
     * Key: userId, Value: SseEmitter
     */
    private final Map<Long, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    /**
     * 创建 SSE 连接
     *
     * @param userId 用户ID
     * @return SseEmitter
     */
    public SseEmitter createConnection(Long userId) {
        log.info("[SSE] 创建连接: userId={}", userId);

        // 如果已存在连接，先移除旧连接
        removeConnection(userId);

        // 创建新的 SseEmitter，设置超时时间
        SseEmitter emitter = new SseEmitter(NotificationConstants.SSE_TIMEOUT);

        // 设置连接完成回调
        emitter.onCompletion(() -> {
            log.info("[SSE] 连接完成: userId={}", userId);
            removeConnection(userId);
        });

        // 设置连接超时回调
        emitter.onTimeout(() -> {
            log.warn("[SSE] 连接超时: userId={}", userId);
            removeConnection(userId);
        });

        // 设置连接错误回调
        emitter.onError((throwable) -> {
            log.error("[SSE] 连接错误: userId={}, error={}", userId, throwable.getMessage());
            removeConnection(userId);
        });

        // 保存连接
        sseEmitters.put(userId, emitter);

        // 发送连接成功消息
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"message\": \"连接成功\", \"timestamp\": " + System.currentTimeMillis() + "}"));
            log.info("[SSE] 连接建立成功: userId={}, 当前连接数={}", userId, sseEmitters.size());
        } catch (IOException e) {
            log.error("[SSE] 发送连接成功消息失败: userId={}", userId, e);
            removeConnection(userId);
        }

        return emitter;
    }

    /**
     * 移除 SSE 连接
     *
     * @param userId 用户ID
     */
    public void removeConnection(Long userId) {
        SseEmitter emitter = sseEmitters.remove(userId);
        if (emitter != null) {
            try {
                emitter.complete();
                log.info("[SSE] 连接已移除: userId={}, 当前连接数={}", userId, sseEmitters.size());
            } catch (Exception e) {
                log.error("[SSE] 关闭连接失败: userId={}", userId, e);
            }
        }
    }

    /**
     * 推送通知到指定用户
     *
     * @param userId       用户ID
     * @param notification 通知消息
     */
    @Async
    public void pushNotificationToUser(Long userId, NotificationMessageDTO notification) {
        SseEmitter emitter = sseEmitters.get(userId);

        if (emitter == null) {
            log.debug("[SSE] 用户未连接，跳过推送: userId={}", userId);
            return;
        }

        try {
            String jsonData = JSON.toJSONString(notification);
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(jsonData));

            log.info("[SSE] 通知推送成功: userId={}, notificationId={}", userId, notification.getId());

        } catch (IOException e) {
            log.error("[SSE] 通知推送失败: userId={}, notificationId={}, error={}",
                    userId, notification.getId(), e.getMessage());
            // 推送失败，移除连接
            removeConnection(userId);
        }
    }

    /**
     * 定时发送心跳，保持连接活跃
     * 每30秒执行一次
     */
    @Scheduled(fixedRate = NotificationConstants.SSE_HEARTBEAT_INTERVAL)
    public void sendHeartbeat() {
        if (sseEmitters.isEmpty()) {
            return;
        }

        log.debug("[SSE] 发送心跳，当前连接数: {}", sseEmitters.size());

        sseEmitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("{\"timestamp\": " + System.currentTimeMillis() + "}"));
            } catch (IOException e) {
                log.warn("[SSE] 心跳发送失败，移除连接: userId={}", userId);
                removeConnection(userId);
            }
        });
    }

    /**
     * 获取当前连接数
     *
     * @return 连接数
     */
    public int getConnectionCount() {
        return sseEmitters.size();
    }

    /**
     * 检查用户是否已连接
     *
     * @param userId 用户ID
     * @return 是否已连接
     */
    public boolean isUserConnected(Long userId) {
        return sseEmitters.containsKey(userId);
    }

    /**
     * 关闭所有连接（应用关闭时调用）
     */
    public void closeAllConnections() {
        log.info("[SSE] 关闭所有连接，总数: {}", sseEmitters.size());

        sseEmitters.forEach((userId, emitter) -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.error("[SSE] 关闭连接失败: userId={}", userId, e);
            }
        });

        sseEmitters.clear();
        log.info("[SSE] 所有连接已关闭");
    }
}
