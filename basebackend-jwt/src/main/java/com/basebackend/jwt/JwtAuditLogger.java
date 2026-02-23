package com.basebackend.jwt;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JWT 审计日志记录器 — 记录所有 Token 相关事件用于安全审计
 * <p>
 * 默认通过 SLF4J 以 INFO 级别记录到专用 logger（jwt.audit）。
 * 可选通过 ApplicationEventPublisher 发布 Spring 事件。
 * <p>
 * 内置可疑活动检测：
 * <ul>
 *   <li>同一用户 5 分钟内刷新 Token 超过 10 次 → SUSPICIOUS_ACTIVITY</li>
 *   <li>同一用户从不同 IP 段登录 → SUSPICIOUS_ACTIVITY</li>
 * </ul>
 */
@Slf4j
public class JwtAuditLogger {

    /** 专用审计 logger */
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("jwt.audit");

    /** 可疑活动检测：刷新滑动窗口（5 分钟） */
    private static final long REFRESH_WINDOW_MILLIS = 5 * 60 * 1000L;
    /** 可疑活动检测：刷新阈值 */
    private static final int REFRESH_THRESHOLD = 10;

    private final boolean enabled;
    private final boolean publishSpringEvents;

    @Nullable
    private final ApplicationEventPublisher eventPublisher;

    /** 滑动窗口：userId -> 刷新时间戳列表 */
    private final Map<String, ConcurrentLinkedDeque<Long>> refreshWindows = new ConcurrentHashMap<>();

    /** IP 记录：userId -> 最近登录 IP */
    private final Map<String, String> recentIps = new ConcurrentHashMap<>();

    public JwtAuditLogger(boolean enabled, boolean publishSpringEvents,
                          @Nullable ApplicationEventPublisher eventPublisher) {
        this.enabled = enabled;
        this.publishSpringEvents = publishSpringEvents;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 记录审计事件
     *
     * @param eventType 事件类型
     * @param userId    用户标识
     * @param deviceId  设备ID（可为 null）
     * @param tokenJti  Token JTI（可为 null）
     * @param details   额外信息（可为 null）
     */
    public void log(JwtAuditEvent eventType, @Nullable String userId,
                    @Nullable String deviceId, @Nullable String tokenJti,
                    @Nullable Map<String, Object> details) {
        if (!enabled) return;

        JwtAuditEntry entry = JwtAuditEntry.builder()
                .eventType(eventType)
                .userId(userId)
                .deviceId(deviceId)
                .tokenJti(tokenJti)
                .timestamp(System.currentTimeMillis())
                .details(details)
                .build();

        // SLF4J 审计日志
        AUDIT_LOG.info("[JWT-AUDIT] event={}, userId={}, deviceId={}, jti={}, details={}",
                eventType, userId, deviceId, tokenJti, details);

        // Spring 事件发布
        if (publishSpringEvents && eventPublisher != null) {
            try {
                eventPublisher.publishEvent(new JwtAuditSpringEvent(this, entry));
            } catch (Exception e) {
                log.warn("Failed to publish JWT audit Spring event: {}", e.getMessage());
            }
        }

        // 可疑活动检测
        if (userId != null) {
            detectSuspiciousActivity(eventType, userId, details);
        }
    }

    /**
     * 记录审计事件（含 IP 和 User-Agent）
     */
    public void log(JwtAuditEvent eventType, @Nullable String userId,
                    @Nullable String deviceId, @Nullable String tokenJti,
                    @Nullable String ip, @Nullable String userAgent,
                    @Nullable Map<String, Object> details) {
        if (!enabled) return;

        JwtAuditEntry entry = JwtAuditEntry.builder()
                .eventType(eventType)
                .userId(userId)
                .deviceId(deviceId)
                .tokenJti(tokenJti)
                .ip(ip)
                .userAgent(userAgent)
                .timestamp(System.currentTimeMillis())
                .details(details)
                .build();

        AUDIT_LOG.info("[JWT-AUDIT] event={}, userId={}, deviceId={}, jti={}, ip={}, details={}",
                eventType, userId, deviceId, tokenJti, ip, details);

        if (publishSpringEvents && eventPublisher != null) {
            try {
                eventPublisher.publishEvent(new JwtAuditSpringEvent(this, entry));
            } catch (Exception e) {
                log.warn("Failed to publish JWT audit Spring event: {}", e.getMessage());
            }
        }

        // IP 异常检测
        if (userId != null && ip != null && eventType == JwtAuditEvent.TOKEN_GENERATED) {
            detectIpAnomaly(userId, ip);
        }

        if (userId != null) {
            detectSuspiciousActivity(eventType, userId, details);
        }
    }

    // ========== 可疑活动检测 ==========

    private void detectSuspiciousActivity(JwtAuditEvent eventType, String userId,
                                          @Nullable Map<String, Object> details) {
        if (eventType == JwtAuditEvent.TOKEN_REFRESHED) {
            detectExcessiveRefresh(userId);
        }
    }

    /**
     * 检测频繁刷新：同一用户 5 分钟内刷新超过 10 次
     */
    private void detectExcessiveRefresh(String userId) {
        ConcurrentLinkedDeque<Long> window = refreshWindows.computeIfAbsent(
                userId, k -> new ConcurrentLinkedDeque<>());

        long now = System.currentTimeMillis();
        window.addLast(now);

        // 清理窗口外的旧记录
        while (!window.isEmpty() && now - window.peekFirst() > REFRESH_WINDOW_MILLIS) {
            window.pollFirst();
        }

        if (window.size() > REFRESH_THRESHOLD) {
            AUDIT_LOG.warn("[JWT-AUDIT] SUSPICIOUS: userId={} refreshed token {} times in {} minutes",
                    userId, window.size(), REFRESH_WINDOW_MILLIS / 60000);
            log(JwtAuditEvent.SUSPICIOUS_ACTIVITY, userId, null, null,
                    Map.of("reason", "excessive_refresh",
                            "count", window.size(),
                            "windowMinutes", REFRESH_WINDOW_MILLIS / 60000));
        }
    }

    /**
     * 检测 IP 段异常：同一用户从不同 IP 段登录
     */
    private void detectIpAnomaly(String userId, String currentIp) {
        String previousIp = recentIps.put(userId, currentIp);
        if (previousIp == null || previousIp.equals(currentIp)) {
            return;
        }

        // 比较前两段（简单实现：A.B.x.x）
        String prevPrefix = getIpPrefix(previousIp);
        String currPrefix = getIpPrefix(currentIp);

        if (prevPrefix != null && currPrefix != null && !prevPrefix.equals(currPrefix)) {
            AUDIT_LOG.warn("[JWT-AUDIT] SUSPICIOUS: userId={} logged in from different IP segment: {} -> {}",
                    userId, previousIp, currentIp);
            log(JwtAuditEvent.SUSPICIOUS_ACTIVITY, userId, null, null,
                    Map.of("reason", "ip_segment_change",
                            "previousIp", previousIp,
                            "currentIp", currentIp));
        }
    }

    /**
     * 提取 IP 前两段（如 192.168.x.x → "192.168"）
     */
    @Nullable
    private static String getIpPrefix(String ip) {
        if (ip == null) return null;
        String[] parts = ip.split("\\.");
        if (parts.length < 2) return null;
        return parts[0] + "." + parts[1];
    }
}
