package com.basebackend.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 业务指标收集器
 * 收集业务相关的自定义指标数据
 *
 * 主要指标:
 * 1. 用户操作指标 (登录、注册、认证)
 * 2. 业务操作指标 (增删改查)
 * 3. 订单指标 (创建、支付、取消)
 * 4. 通知指标 (发送、失败)
 * 5. 第三方集成指标 (API 调用、成功率)
 *
 * @author basebackend team
 * @version 1.0
 */
public class BusinessMetrics {

    private final MeterRegistry registry;
    private final List<Tag> commonTags;

    // 用户相关指标
    private final Counter userLoginSuccessCounter;
    private final Counter userLoginFailureCounter;
    private final Counter userRegistrationCounter;
    private final Timer userLoginTimer;
    private final AtomicInteger activeUsers = new AtomicInteger(0);

    // 业务操作指标
    private final Counter businessOperationCounter;
    private final Timer businessOperationTimer;
    private final DistributionSummary businessOperationSummary;

    // 认证授权指标
    private final Counter authAttemptCounter;
    private final Counter authSuccessCounter;
    private final Counter authFailureCounter;
    private final Timer authTimer;

    // 通知指标
    private final Counter notificationSentCounter;
    private final Counter notificationFailedCounter;
    private final Timer notificationTimer;

    // 第三方 API 指标
    private final Counter externalApiCallCounter;
    private final Counter externalApiSuccessCounter;
    private final Counter externalApiFailureCounter;
    private final Timer externalApiTimer;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;

        // 添加通用标签
        this.commonTags = new ArrayList<>();
        commonTags.add(Tag.of("category", "business"));

        // 用户指标
        this.userLoginSuccessCounter = Counter.builder("user.login.success")
            .description("User login successful attempts")
            .tags(commonTags)
            .register(registry);

        this.userLoginFailureCounter = Counter.builder("user.login.failure")
            .description("User login failed attempts")
            .tags(commonTags)
            .register(registry);

        this.userRegistrationCounter = Counter.builder("user.registration")
            .description("User registration attempts")
            .tags(commonTags)
            .register(registry);

        this.userLoginTimer = Timer.builder("user.login.duration")
            .description("User login time")
            .tags(commonTags)
            .register(registry);

        this.activeUsers = new AtomicInteger(0);
        Gauge.builder("user.active")
            .description("Number of active users")
            .tags(commonTags)
            .register(registry, activeUsers, AtomicInteger::get);

        // 业务操作指标
        this.businessOperationCounter = Counter.builder("business.operation")
            .description("Business operation execution count")
            .tags(commonTags)
            .register(registry);

        this.businessOperationTimer = Timer.builder("business.operation.duration")
            .description("Business operation execution time")
            .tags(commonTags)
            .register(registry);

        this.businessOperationSummary = DistributionSummary.builder("business.operation.size")
            .description("Business operation data size")
            .tags(commonTags)
            .register(registry);

        // 认证指标
        this.authAttemptCounter = Counter.builder("auth.attempt")
            .description("Authentication attempts")
            .tags(commonTags)
            .register(registry);

        this.authSuccessCounter = Counter.builder("auth.success")
            .description("Authentication successful attempts")
            .tags(commonTags)
            .register(registry);

        this.authFailureCounter = Counter.builder("auth.failure")
            .description("Authentication failed attempts")
            .tags(commonTags)
            .register(registry);

        this.authTimer = Timer.builder("auth.duration")
            .description("Authentication time")
            .tags(commonTags)
            .register(registry);

        // 通知指标
        this.notificationSentCounter = Counter.builder("notification.sent")
            .description("Notifications sent successfully")
            .tags(commonTags)
            .register(registry);

        this.notificationFailedCounter = Counter.builder("notification.failed")
            .description("Notifications failed to send")
            .tags(commonTags)
            .register(registry);

        this.notificationTimer = Timer.builder("notification.duration")
            .description("Notification sending time")
            .tags(commonTags)
            .register(registry);

        // 第三方 API 指标
        this.externalApiCallCounter = Counter.builder("external.api.call")
            .description("External API calls")
            .tags(commonTags)
            .register(registry);

        this.externalApiSuccessCounter = Counter.builder("external.api.success")
            .description("External API successful calls")
            .tags(commonTags)
            .register(registry);

        this.externalApiFailureCounter = Counter.builder("external.api.failure")
            .description("External API failed calls")
            .tags(commonTags)
            .register(registry);

        this.externalApiTimer = Timer.builder("external.api.duration")
            .description("External API call time")
            .tags(commonTags)
            .register(registry);
    }

    // ==================== 用户相关指标 ====================

    /**
     * 记录用户登录成功
     */
    public void recordUserLoginSuccess(String userId, long durationMs) {
        userLoginSuccessCounter.increment();
        userLoginTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        // 添加用户 ID 标签
        userLoginSuccessCounter.increment(Tag.of("user_id", userId));
    }

    /**
     * 记录用户登录失败
     */
    public void recordUserLoginFailure(String userId, String reason) {
        userLoginFailureCounter.increment();
        userLoginFailureCounter.increment(Tag.of("user_id", userId));
        userLoginFailureCounter.increment(Tag.of("reason", reason));
    }

    /**
     * 记录用户注册
     */
    public void recordUserRegistration(String source) {
        userRegistrationCounter.increment();
        userRegistrationCounter.increment(Tag.of("source", source));
    }

    /**
     * 增加活跃用户数
     */
    public void incrementActiveUsers() {
        activeUsers.incrementAndGet();
    }

    /**
     * 减少活跃用户数
     */
    public void decrementActiveUsers() {
        activeUsers.decrementAndGet();
    }

    // ==================== 业务操作指标 ====================

    /**
     * 记录业务操作
     */
    public void recordBusinessOperation(String operation, String entityType, String entityId,
                                      long durationMs, long dataSize) {
        businessOperationCounter.increment();
        businessOperationCounter.increment(Tag.of("operation", operation));
        businessOperationCounter.increment(Tag.of("entity_type", entityType));
        businessOperationCounter.increment(Tag.of("entity_id", entityId));

        businessOperationTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        if (dataSize > 0) {
            businessOperationSummary.record(dataSize);
            businessOperationSummary.record(Tag.of("operation", operation), dataSize);
        }
    }

    /**
     * 记录业务操作开始
     */
    public Timer.Sample startBusinessOperationTimer() {
        return Timer.start(registry);
    }

    /**
     * 记录业务操作完成
     */
    public void stopBusinessOperationTimer(Timer.Sample sample, String operation, String entityType) {
        sample.stop(businessOperationTimer);
        businessOperationTimer.record(Tag.of("operation", operation));
        businessOperationTimer.record(Tag.of("entity_type", entityType));
    }

    // ==================== 认证指标 ====================

    /**
     * 记录认证尝试
     */
    public void recordAuthAttempt(String method, String userId) {
        authAttemptCounter.increment();
        authAttemptCounter.increment(Tag.of("method", method));
        if (userId != null) {
            authAttemptCounter.increment(Tag.of("user_id", userId));
        }
    }

    /**
     * 记录认证成功
     */
    public void recordAuthSuccess(String method, String userId, long durationMs) {
        authSuccessCounter.increment();
        authSuccessCounter.increment(Tag.of("method", method));
        if (userId != null) {
            authSuccessCounter.increment(Tag.of("user_id", userId));
        }

        authTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * 记录认证失败
     */
    public void recordAuthFailure(String method, String userId, String reason) {
        authFailureCounter.increment();
        authFailureCounter.increment(Tag.of("method", method));
        if (userId != null) {
            authFailureCounter.increment(Tag.of("user_id", userId));
        }
        authFailureCounter.increment(Tag.of("reason", reason));
    }

    // ==================== 通知指标 ====================

    /**
     * 记录通知发送成功
     */
    public void recordNotificationSent(String type, String channel) {
        notificationSentCounter.increment();
        notificationSentCounter.increment(Tag.of("type", type));
        notificationSentCounter.increment(Tag.of("channel", channel));
    }

    /**
     * 记录通知发送失败
     */
    public void recordNotificationFailed(String type, String channel, String reason) {
        notificationFailedCounter.increment();
        notificationFailedCounter.increment(Tag.of("type", type));
        notificationFailedCounter.increment(Tag.of("channel", channel));
        notificationFailedCounter.increment(Tag.of("reason", reason));
    }

    /**
     * 记录通知发送耗时
     */
    public void recordNotificationDuration(String type, String channel, long durationMs) {
        notificationTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        notificationTimer.record(Tag.of("type", type));
        notificationTimer.record(Tag.of("channel", channel));
    }

    // ==================== 第三方 API 指标 ====================

    /**
     * 记录第三方 API 调用
     */
    public void recordExternalApiCall(String provider, String endpoint) {
        externalApiCallCounter.increment();
        externalApiCallCounter.increment(Tag.of("provider", provider));
        externalApiCallCounter.increment(Tag.of("endpoint", endpoint));
    }

    /**
     * 记录第三方 API 调用成功
     */
    public void recordExternalApiSuccess(String provider, String endpoint, long durationMs) {
        externalApiSuccessCounter.increment();
        externalApiSuccessCounter.increment(Tag.of("provider", provider));
        externalApiSuccessCounter.increment(Tag.of("endpoint", endpoint));

        externalApiTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * 记录第三方 API 调用失败
     */
    public void recordExternalApiFailure(String provider, String endpoint, String reason) {
        externalApiFailureCounter.increment();
        externalApiFailureCounter.increment(Tag.of("provider", provider));
        externalApiFailureCounter.increment(Tag.of("endpoint", endpoint));
        externalApiFailureCounter.increment(Tag.of("reason", reason));
    }

    // ==================== 工具方法 ====================

    /**
     * 重置所有计数器
     */
    public void resetCounters() {
        try {
            // 通过重新创建计数器来重置 (Micrometer 不支持重置现有计数器)
            // 这里只是示例，实际实现可能需要重新注册 Meter
        } catch (MeterNotFoundException e) {
            // 忽略不存在指标的异常
        }
    }

    /**
     * 获取指标值
     */
    public double getMetricValue(String metricName) {
        try {
            return registry.find(metricName).value();
        } catch (MeterNotFoundException e) {
            return 0.0;
        }
    }
}
