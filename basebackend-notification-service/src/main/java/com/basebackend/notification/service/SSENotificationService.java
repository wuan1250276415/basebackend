package com.basebackend.notification.service;

import com.alibaba.fastjson2.JSON;
import com.basebackend.common.enums.CommonErrorCode;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.notification.config.NotificationSecurityConfig;
import com.basebackend.notification.constants.NotificationConstants;
import com.basebackend.notification.dto.NotificationMessageDTO;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SSE 通知推送服务
 * P1: 增强连接管理，添加连接数限制和监控
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Slf4j
@Service
public class SSENotificationService {

    private final NotificationSecurityConfig securityConfig;

    /**
     * 存储用户 SSE 连接
     * Key: userId, Value: SseEmitter
     */
    private final Map<Long, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    /**
     * 连接计数器（用于监控）
     */
    private final AtomicInteger connectionCounter = new AtomicInteger(0);

    /**
     * 推送成功计数
     */
    private final AtomicInteger pushSuccessCounter = new AtomicInteger(0);

    /**
     * 推送失败计数
     */
    private final AtomicInteger pushFailureCounter = new AtomicInteger(0);

    public SSENotificationService(NotificationSecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    /**
     * 创建 SSE 连接
     * P1: 添加连接数限制检查
     *
     * @param userId 用户ID
     * @return SseEmitter
     */
    public SseEmitter createConnection(Long userId) {
        // P1: 连接数限制检查
        if (sseEmitters.size() >= securityConfig.getSseMaxConnections()) {
            log.warn("[SSE] 连接数已达上限: current={}, max={}", 
                    sseEmitters.size(), securityConfig.getSseMaxConnections());
            throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE, "服务繁忙，请稍后重试");
        }

        // P2: 日志脱敏 - 不记录完整userId
        log.info("[SSE] 创建连接: userId=***{}", userId % 10000);

        // 如果已存在连接，先移除旧连接
        removeConnection(userId);

        // 创建新的 SseEmitter，设置超时时间
        SseEmitter emitter = new SseEmitter(NotificationConstants.SSE_TIMEOUT);

        // 设置连接完成回调
        emitter.onCompletion(() -> {
            log.debug("[SSE] 连接完成: userId=***{}", userId % 10000);
            doRemoveConnection(userId);
        });

        // 设置连接超时回调
        emitter.onTimeout(() -> {
            log.debug("[SSE] 连接超时: userId=***{}", userId % 10000);
            doRemoveConnection(userId);
        });

        // 设置连接错误回调
        emitter.onError((throwable) -> {
            // P1: 不暴露详细错误信息
            log.warn("[SSE] 连接错误: userId=***{}", userId % 10000);
            doRemoveConnection(userId);
        });

        // 保存连接
        sseEmitters.put(userId, emitter);
        connectionCounter.incrementAndGet();

        // 发送连接成功消息
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"message\": \"连接成功\", \"timestamp\": " + System.currentTimeMillis() + "}"));
            log.info("[SSE] 连接建立成功: 当前连接数={}", sseEmitters.size());
        } catch (IOException e) {
            log.warn("[SSE] 发送连接成功消息失败");
            doRemoveConnection(userId);
        }

        return emitter;
    }

    /**
     * 移除 SSE 连接（公开方法）
     */
    public void removeConnection(Long userId) {
        doRemoveConnection(userId);
    }

    /**
     * 内部移除连接方法
     */
    private void doRemoveConnection(Long userId) {
        SseEmitter emitter = sseEmitters.remove(userId);
        if (emitter != null) {
            try {
                emitter.complete();
                connectionCounter.decrementAndGet();
                log.debug("[SSE] 连接已移除: 当前连接数={}", sseEmitters.size());
            } catch (Exception e) {
                // P1: 不记录详细异常堆栈
                log.debug("[SSE] 关闭连接时发生异常");
            }
        }
    }

    /**
     * 推送通知到指定用户
     * P1: 增强异常处理，添加推送统计
     */
    @Async
    public void pushNotificationToUser(Long userId, NotificationMessageDTO notification) {
        SseEmitter emitter = sseEmitters.get(userId);

        if (emitter == null) {
            log.debug("[SSE] 用户未连接，跳过推送");
            return;
        }

        try {
            String jsonData = JSON.toJSONString(notification);
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(jsonData));

            pushSuccessCounter.incrementAndGet();
            log.debug("[SSE] 通知推送成功: notificationId={}", notification.getId());

        } catch (IOException e) {
            pushFailureCounter.incrementAndGet();
            // P1: 不暴露详细错误信息
            log.warn("[SSE] 通知推送失败: notificationId={}", notification.getId());
            doRemoveConnection(userId);
        } catch (Exception e) {
            pushFailureCounter.incrementAndGet();
            log.warn("[SSE] 通知推送异常: notificationId={}", notification.getId());
            doRemoveConnection(userId);
        }
    }

    /**
     * 定时发送心跳，保持连接活跃
     */
    @Scheduled(fixedRate = NotificationConstants.SSE_HEARTBEAT_INTERVAL)
    public void sendHeartbeat() {
        if (sseEmitters.isEmpty()) {
            return;
        }

        log.debug("[SSE] 发送心跳，当前连接数: {}", sseEmitters.size());

        // P1: 使用副本遍历，避免并发修改
        sseEmitters.entrySet().stream().toList().forEach(entry -> {
            try {
                entry.getValue().send(SseEmitter.event()
                        .name("heartbeat")
                        .data("{\"timestamp\": " + System.currentTimeMillis() + "}"));
            } catch (IOException e) {
                log.debug("[SSE] 心跳发送失败，移除连接");
                doRemoveConnection(entry.getKey());
            }
        });
    }

    /**
     * 获取当前连接数
     */
    public int getConnectionCount() {
        return sseEmitters.size();
    }

    /**
     * 检查用户是否已连接
     */
    public boolean isUserConnected(Long userId) {
        return sseEmitters.containsKey(userId);
    }

    /**
     * 获取推送成功计数
     */
    public int getPushSuccessCount() {
        return pushSuccessCounter.get();
    }

    /**
     * 获取推送失败计数
     */
    public int getPushFailureCount() {
        return pushFailureCounter.get();
    }

    /**
     * 获取历史连接总数
     */
    public int getTotalConnectionCount() {
        return connectionCounter.get();
    }

    /**
     * 关闭所有连接（应用关闭时调用）
     */
    @PreDestroy
    public void closeAllConnections() {
        log.info("[SSE] 关闭所有连接，总数: {}", sseEmitters.size());

        sseEmitters.entrySet().stream().toList().forEach(entry -> {
            try {
                entry.getValue().complete();
            } catch (Exception e) {
                log.debug("[SSE] 关闭连接时发生异常");
            }
        });

        sseEmitters.clear();
        log.info("[SSE] 所有连接已关闭");
    }
}
