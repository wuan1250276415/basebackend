package com.basebackend.notification.service;

import com.basebackend.common.enums.CommonErrorCode;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.common.util.JsonUtils;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SSE 通知推送服务
 * <p>
 * 安全改进：
 * <ul>
 *   <li>短命令牌（Short-lived stream token）交换机制，避免 JWT 出现在 URL 访问日志</li>
 *   <li>使用 {@link ConcurrentHashMap#remove(Object, Object)} 原子性地移除连接，消除竞态</li>
 *   <li>连接数限制检查与写入在同步块内执行，修复 TOCTOU 漏洞</li>
 *   <li>connectionCounter 改为只增的历史总数，当前连接数直接取 sseEmitters.size()</li>
 * </ul>
 */
@Slf4j
@Service
public class SSENotificationService {

    private final NotificationSecurityConfig securityConfig;

    /** 活跃 SSE 连接：userId → SseEmitter */
    private final Map<Long, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    /** 短命令牌库：token → StreamTokenEntry（30 秒有效，一次性消费） */
    private final Map<String, StreamTokenEntry> streamTokens = new ConcurrentHashMap<>();

    /** 历史连接总数（只增不减，供监控使用） */
    private final AtomicLong totalConnectionCounter = new AtomicLong(0);

    /** 推送成功计数 */
    private final AtomicLong pushSuccessCounter = new AtomicLong(0);

    /** 推送失败计数 */
    private final AtomicLong pushFailureCounter = new AtomicLong(0);

    /** 连接操作互斥锁（保证 size 检查与 put 的原子性） */
    private final Object connectionLock = new Object();

    private static final long STREAM_TOKEN_TTL_MS = 30_000L;

    public SSENotificationService(NotificationSecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    // -------------------------------------------------------------------------
    // 短命令牌管理（B2）
    // -------------------------------------------------------------------------

    /**
     * 生成一个 30 秒有效的一次性 SSE 连接令牌。
     * 客户端应先调用此接口（通过 Authorization 头认证），再凭令牌建立 SSE 连接。
     *
     * @param userId 已认证的用户 ID
     * @return 不透明的一次性令牌字符串
     */
    public String generateStreamToken(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        streamTokens.put(token, new StreamTokenEntry(userId, System.currentTimeMillis() + STREAM_TOKEN_TTL_MS));
        log.debug("[SSE] 生成连接令牌: userId=***{}", Math.abs(userId % 10000));
        return token;
    }

    /**
     * 验证并消费一次性 SSE 令牌，返回对应的 userId。
     * 令牌已过期或不存在时抛出 BusinessException。
     *
     * @param token 客户端传入的令牌
     * @return 令牌对应的用户 ID
     */
    public Long validateAndConsumeStreamToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "缺少连接令牌");
        }
        StreamTokenEntry entry = streamTokens.remove(token);
        if (entry == null || System.currentTimeMillis() > entry.expireAt()) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "连接令牌无效或已过期");
        }
        return entry.userId();
    }

    /** 定时清理已过期但未被消费的令牌（每分钟一次） */
    @Scheduled(fixedRate = 60_000)
    public void cleanExpiredStreamTokens() {
        long now = System.currentTimeMillis();
        int removed = 0;
        for (Map.Entry<String, StreamTokenEntry> e : streamTokens.entrySet()) {
            if (now > e.getValue().expireAt() && streamTokens.remove(e.getKey(), e.getValue())) {
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("[SSE] 清理过期令牌: count={}", removed);
        }
    }

    // -------------------------------------------------------------------------
    // SSE 连接管理（H1 + H2）
    // -------------------------------------------------------------------------

    /**
     * 创建 SSE 连接。
     * <p>
     * 使用 {@code connectionLock} 保证「连接数检查 → put」的原子性（修复 TOCTOU）；
     * 使用 {@link ConcurrentHashMap#remove(Object, Object)} 确保回调只移除自身对应的连接。
     *
     * @param userId 用户 ID
     * @return SseEmitter
     */
    public SseEmitter createConnection(Long userId) {
        int maxConn = securityConfig.getSseMaxConnections();

        SseEmitter emitter = new SseEmitter(NotificationConstants.SSE_TIMEOUT);
        // 回调捕获 emitter 引用，用 remove(key, value) 保证只移除自身连接
        emitter.onCompletion(() -> removeConnectionIfPresent(userId, emitter));
        emitter.onTimeout(()   -> removeConnectionIfPresent(userId, emitter));
        emitter.onError(t      -> removeConnectionIfPresent(userId, emitter));

        SseEmitter old;
        synchronized (connectionLock) {
            // 注意：仅当替换同一用户的旧连接时 size 不变；新用户才需检查上限
            boolean isNewUser = !sseEmitters.containsKey(userId);
            if (isNewUser && sseEmitters.size() >= maxConn) {
                log.warn("[SSE] 连接数已达上限: current={}, max={}", sseEmitters.size(), maxConn);
                throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE, "服务繁忙，请稍后重试");
            }
            old = sseEmitters.put(userId, emitter);
            totalConnectionCounter.incrementAndGet();
        }

        // 在锁外关闭旧连接（旧连接的 onCompletion 调用 remove(userId, old)，
        // 此时 map 已存放新 emitter，remove(userId, old) 返回 false → 安全无副作用）
        if (old != null) {
            try { old.complete(); } catch (Exception ignored) { /* 已替换，忽略 */ }
        }

        log.info("[SSE] 连接建立: userId=***{}, 当前连接数={}", Math.abs(userId % 10000), sseEmitters.size());

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"message\":\"连接成功\",\"timestamp\":" + System.currentTimeMillis() + "}"));
        } catch (IOException e) {
            removeConnectionIfPresent(userId, emitter);
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR, "SSE 连接建立失败");
        }

        return emitter;
    }

    /**
     * 原子性地移除指定连接（仅当 map 中存储的仍是传入的 emitter 实例时才移除）。
     */
    private void removeConnectionIfPresent(Long userId, SseEmitter emitter) {
        if (sseEmitters.remove(userId, emitter)) {
            log.debug("[SSE] 连接已移除: userId=***{}, 当前连接数={}", Math.abs(userId % 10000), sseEmitters.size());
        }
    }

    /**
     * 主动关闭指定用户的 SSE 连接（如强制下线场景）。
     */
    public void removeConnection(Long userId) {
        SseEmitter emitter = sseEmitters.remove(userId);
        if (emitter != null) {
            try { emitter.complete(); } catch (Exception ignored) { }
        }
    }

    // -------------------------------------------------------------------------
    // 推送与心跳
    // -------------------------------------------------------------------------

    /**
     * 异步推送通知到指定用户。
     */
    @Async
    public void pushNotificationToUser(Long userId, NotificationMessageDTO notification) {
        SseEmitter emitter = sseEmitters.get(userId);
        if (emitter == null) {
            log.debug("[SSE] 用户未连接，跳过推送: userId=***{}", Math.abs(userId % 10000));
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(JsonUtils.toJsonString(notification)));
            pushSuccessCounter.incrementAndGet();
            log.debug("[SSE] 推送成功: notificationId={}", notification.id());
        } catch (IOException e) {
            pushFailureCounter.incrementAndGet();
            log.warn("[SSE] 推送失败: notificationId={}", notification.id());
            removeConnectionIfPresent(userId, emitter);
        } catch (Exception e) {
            pushFailureCounter.incrementAndGet();
            log.warn("[SSE] 推送异常: notificationId={}", notification.id());
            removeConnectionIfPresent(userId, emitter);
        }
    }

    /**
     * 定时心跳，保持所有连接活跃（每 30 秒）。
     */
    @Scheduled(fixedRate = NotificationConstants.SSE_HEARTBEAT_INTERVAL)
    public void sendHeartbeat() {
        if (sseEmitters.isEmpty()) {
            return;
        }
        log.debug("[SSE] 发送心跳，当前连接数: {}", sseEmitters.size());
        String heartbeatData = "{\"timestamp\":" + System.currentTimeMillis() + "}";

        for (Map.Entry<Long, SseEmitter> entry : sseEmitters.entrySet()) {
            try {
                entry.getValue().send(SseEmitter.event().name("heartbeat").data(heartbeatData));
            } catch (IOException e) {
                log.debug("[SSE] 心跳失败，移除连接: userId=***{}", entry.getKey() % 10000);
                removeConnectionIfPresent(entry.getKey(), entry.getValue());
            }
        }
    }

    // -------------------------------------------------------------------------
    // 优雅关闭
    // -------------------------------------------------------------------------

    @PreDestroy
    public void closeAllConnections() {
        log.info("[SSE] 关闭所有连接，当前数: {}", sseEmitters.size());
        // 先快照再清空，避免并发修改
        List<SseEmitter> snapshot = new ArrayList<>(sseEmitters.values());
        sseEmitters.clear();
        for (SseEmitter emitter : snapshot) {
            try { emitter.complete(); } catch (Exception ignored) { }
        }
        log.info("[SSE] 所有连接已关闭");
    }

    // -------------------------------------------------------------------------
    // 监控指标
    // -------------------------------------------------------------------------

    public int getConnectionCount() {
        return sseEmitters.size();
    }

    public long getTotalConnectionCount() {
        return totalConnectionCounter.get();
    }

    public long getPushSuccessCount() {
        return pushSuccessCounter.get();
    }

    public long getPushFailureCount() {
        return pushFailureCounter.get();
    }

    public boolean isUserConnected(Long userId) {
        return sseEmitters.containsKey(userId);
    }

    // -------------------------------------------------------------------------
    // 内部记录
    // -------------------------------------------------------------------------

    private record StreamTokenEntry(Long userId, long expireAt) { }
}
