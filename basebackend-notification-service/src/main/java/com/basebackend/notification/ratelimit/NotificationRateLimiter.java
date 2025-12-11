package com.basebackend.notification.ratelimit;

import com.basebackend.common.enums.CommonErrorCode;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.notification.config.NotificationSecurityConfig;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 通知服务限流器
 * P2: 接口限流保护
 */
@Slf4j
@Component
public class NotificationRateLimiter {

    private final LoadingCache<Long, RateLimiter> emailRateLimiters;
    private final LoadingCache<Long, RateLimiter> notificationRateLimiters;
    private final RateLimiter globalEmailRateLimiter;
    private final RateLimiter globalNotificationRateLimiter;

    public NotificationRateLimiter(NotificationSecurityConfig config) {
        this.globalEmailRateLimiter = RateLimiter.create(config.getEmailRateLimitPerMinute() / 60.0);
        this.globalNotificationRateLimiter = RateLimiter.create(config.getNotificationRateLimitPerMinute() / 60.0);

        this.emailRateLimiters = CacheBuilder.newBuilder()
                .maximumSize(10000)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build(new CacheLoader<Long, RateLimiter>() {
                    @Override
                    public RateLimiter load(Long userId) {
                        return RateLimiter.create(10.0 / 60.0);
                    }
                });

        this.notificationRateLimiters = CacheBuilder.newBuilder()
                .maximumSize(10000)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build(new CacheLoader<Long, RateLimiter>() {
                    @Override
                    public RateLimiter load(Long userId) {
                        return RateLimiter.create(100.0 / 60.0);
                    }
                });
    }

    public void checkEmailRateLimit(Long userId) {
        if (!globalEmailRateLimiter.tryAcquire()) {
            log.warn("[限流] 邮件发送全局限流触发");
            throw new BusinessException(CommonErrorCode.TOO_MANY_REQUESTS, "系统繁忙，请稍后再试");
        }
        if (userId != null) {
            try {
                RateLimiter limiter = emailRateLimiters.get(userId);
                if (!limiter.tryAcquire()) {
                    log.warn("[限流] 用户邮件发送限流触发");
                    throw new BusinessException(CommonErrorCode.TOO_MANY_REQUESTS, "邮件发送过于频繁");
                }
            } catch (ExecutionException e) {
                log.error("[限流] 获取限流器失败", e);
            }
        }
    }

    public void checkNotificationRateLimit(Long userId) {
        if (!globalNotificationRateLimiter.tryAcquire()) {
            log.warn("[限流] 通知创建全局限流触发");
            throw new BusinessException(CommonErrorCode.TOO_MANY_REQUESTS, "系统繁忙，请稍后再试");
        }
        if (userId != null) {
            try {
                RateLimiter limiter = notificationRateLimiters.get(userId);
                if (!limiter.tryAcquire()) {
                    log.warn("[限流] 用户通知创建限流触发");
                    throw new BusinessException(CommonErrorCode.TOO_MANY_REQUESTS, "操作过于频繁");
                }
            } catch (ExecutionException e) {
                log.error("[限流] 获取限流器失败", e);
            }
        }
    }
}
