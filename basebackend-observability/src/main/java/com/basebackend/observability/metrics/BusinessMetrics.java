package com.basebackend.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 业务指标收集器
 * 提供业务相关的指标采集能力（用户、订单、支付等）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessMetrics {

    private final MeterRegistry meterRegistry;

    // 用户相关指标
    private final AtomicLong totalUsers = new AtomicLong(0);
    private final AtomicLong activeUsers = new AtomicLong(0);
    private final AtomicLong onlineUsers = new AtomicLong(0);

    // 订单相关指标
    private final AtomicLong totalOrders = new AtomicLong(0);
    private final AtomicLong pendingOrders = new AtomicLong(0);
    private final AtomicLong completedOrders = new AtomicLong(0);

    // 缓存统计
    private final ConcurrentHashMap<String, AtomicLong> cacheHits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> cacheMisses = new ConcurrentHashMap<>();

    /**
     * 初始化业务指标 Gauges
     */
    public void initBusinessMetrics() {
        // 用户指标
        Gauge.builder("business_users_total", totalUsers, AtomicLong::get)
                .description("Total number of registered users")
                .register(meterRegistry);

        Gauge.builder("business_users_active", activeUsers, AtomicLong::get)
                .description("Number of active users (last 30 days)")
                .register(meterRegistry);

        Gauge.builder("business_users_online", onlineUsers, AtomicLong::get)
                .description("Number of currently online users")
                .register(meterRegistry);

        // 订单指标
        Gauge.builder("business_orders_total", totalOrders, AtomicLong::get)
                .description("Total number of orders")
                .register(meterRegistry);

        Gauge.builder("business_orders_pending", pendingOrders, AtomicLong::get)
                .description("Number of pending orders")
                .register(meterRegistry);

        Gauge.builder("business_orders_completed", completedOrders, AtomicLong::get)
                .description("Number of completed orders")
                .register(meterRegistry);

        log.info("Business metrics initialized successfully");
    }

    // ==================== 用户相关指标 ====================

    /**
     * 记录用户注册
     */
    public void recordUserRegistration(String source, boolean success) {
        Counter.builder("business_user_registrations_total")
                .description("Total user registrations")
                .tag("source", source)
                .tag("status", success ? "success" : "failed")
                .register(meterRegistry)
                .increment();

        if (success) {
            totalUsers.incrementAndGet();
        }
    }

    /**
     * 记录用户登录
     */
    public void recordUserLogin(String method, boolean success) {
        Counter.builder("business_user_logins_total")
                .description("Total user login attempts")
                .tag("method", method)  // password, sms, oauth, etc.
                .tag("status", success ? "success" : "failed")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录用户登出
     */
    public void recordUserLogout() {
        Counter.builder("business_user_logouts_total")
                .description("Total user logouts")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 更新在线用户数
     */
    public void updateOnlineUsers(long count) {
        onlineUsers.set(count);
    }

    /**
     * 更新活跃用户数
     */
    public void updateActiveUsers(long count) {
        activeUsers.set(count);
    }

    /**
     * 更新总用户数
     */
    public void updateTotalUsers(long count) {
        totalUsers.set(count);
    }

    // ==================== 订单相关指标 ====================

    /**
     * 记录订单创建
     */
    public void recordOrderCreation(String orderType, boolean success) {
        Counter.builder("business_order_creations_total")
                .description("Total order creations")
                .tag("type", orderType)
                .tag("status", success ? "success" : "failed")
                .register(meterRegistry)
                .increment();

        if (success) {
            totalOrders.incrementAndGet();
            pendingOrders.incrementAndGet();
        }
    }

    /**
     * 记录订单完成
     */
    public void recordOrderCompletion(String orderType, long processingTimeMs) {
        Counter.builder("business_order_completions_total")
                .description("Total completed orders")
                .tag("type", orderType)
                .register(meterRegistry)
                .increment();

        Timer.builder("business_order_processing_time_seconds")
                .description("Order processing time in seconds")
                .tag("type", orderType)
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(processingTimeMs));

        pendingOrders.decrementAndGet();
        completedOrders.incrementAndGet();
    }

    /**
     * 记录订单取消
     */
    public void recordOrderCancellation(String orderType, String reason) {
        Counter.builder("business_order_cancellations_total")
                .description("Total cancelled orders")
                .tag("type", orderType)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();

        pendingOrders.decrementAndGet();
    }

    /**
     * 更新待处理订单数
     */
    public void updatePendingOrders(long count) {
        pendingOrders.set(count);
    }

    // ==================== 支付相关指标 ====================

    /**
     * 记录支付请求
     */
    public void recordPaymentRequest(String paymentMethod, String currency, double amount) {
        Counter.builder("business_payment_requests_total")
                .description("Total payment requests")
                .tag("method", paymentMethod)
                .tag("currency", currency)
                .register(meterRegistry)
                .increment();

        // 记录支付金额（使用分布汇总）
        Timer.builder("business_payment_amount_distribution")
                .description("Payment amount distribution")
                .tag("method", paymentMethod)
                .tag("currency", currency)
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis((long) (amount * 1000)));
    }

    /**
     * 记录支付成功
     */
    public void recordPaymentSuccess(String paymentMethod, String currency, double amount) {
        Counter.builder("business_payment_success_total")
                .description("Total successful payments")
                .tag("method", paymentMethod)
                .tag("currency", currency)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录支付失败
     */
    public void recordPaymentFailure(String paymentMethod, String reason) {
        Counter.builder("business_payment_failures_total")
                .description("Total failed payments")
                .tag("method", paymentMethod)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    // ==================== 消息相关指标 ====================

    /**
     * 记录消息发送
     */
    public void recordMessageSent(String messageType, String channel, boolean success) {
        Counter.builder("business_messages_sent_total")
                .description("Total messages sent")
                .tag("type", messageType)  // email, sms, push, etc.
                .tag("channel", channel)
                .tag("status", success ? "success" : "failed")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录通知推送
     */
    public void recordNotificationPushed(String notificationType, boolean success) {
        Counter.builder("business_notifications_pushed_total")
                .description("Total notifications pushed")
                .tag("type", notificationType)
                .tag("status", success ? "success" : "failed")
                .register(meterRegistry)
                .increment();
    }

    // ==================== 缓存相关指标 ====================

    /**
     * 记录缓存命中率
     */
    public void recordCacheAccess(String cacheName, boolean hit) {
        if (hit) {
            cacheHits.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
        } else {
            cacheMisses.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
        }

        // 计算命中率
        long hits = cacheHits.getOrDefault(cacheName, new AtomicLong(0)).get();
        long misses = cacheMisses.getOrDefault(cacheName, new AtomicLong(0)).get();
        long total = hits + misses;

        if (total > 0) {
            double hitRate = (double) hits / total;
            Gauge.builder("business_cache_hit_rate", () -> hitRate)
                    .description("Cache hit rate")
                    .tag("cache", cacheName)
                    .register(meterRegistry);
        }
    }

    // ==================== 业务操作指标 ====================

    /**
     * 记录业务操作耗时
     */
    public void recordBusinessOperationTime(String operationType, String entity, long durationMs) {
        Timer.builder("business_operation_time_seconds")
                .description("Business operation execution time")
                .tag("operation", operationType)  // create, update, delete, query
                .tag("entity", entity)  // user, order, product, etc.
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(durationMs));
    }

    /**
     * 记录业务操作结果
     */
    public void recordBusinessOperationResult(String operationType, String entity, boolean success) {
        Counter.builder("business_operations_total")
                .description("Total business operations")
                .tag("operation", operationType)
                .tag("entity", entity)
                .tag("status", success ? "success" : "failed")
                .register(meterRegistry)
                .increment();
    }

    // ==================== API 使用统计 ====================

    /**
     * 记录 API 调用量（按用户）
     */
    public void recordApiCallByUser(String userId, String endpoint) {
        Counter.builder("business_api_calls_by_user_total")
                .description("API calls grouped by user")
                .tag("user_id", userId)
                .tag("endpoint", endpoint)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录 API 限流事件
     */
    public void recordRateLimitEvent(String userId, String endpoint) {
        Counter.builder("business_rate_limit_events_total")
                .description("Rate limit events")
                .tag("user_id", userId)
                .tag("endpoint", endpoint)
                .register(meterRegistry)
                .increment();
    }
}
