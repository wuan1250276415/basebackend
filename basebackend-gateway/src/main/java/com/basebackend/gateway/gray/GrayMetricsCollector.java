package com.basebackend.gateway.gray;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 灰度发布监控指标收集器
 * <p>
 * 使用 Micrometer 收集灰度发布相关的监控指标，支持：
 * <ul>
 * <li>版本流量分布统计</li>
 * <li>灰度策略命中率</li>
 * <li>路由决策耗时</li>
 * <li>实例选择统计</li>
 * </ul>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnClass(MeterRegistry.class)
public class GrayMetricsCollector {

    private final MeterRegistry meterRegistry;

    /**
     * 度量名称前缀
     */
    private static final String METRIC_PREFIX = "gateway.gray";

    /**
     * 版本请求计数器缓存
     */
    private final Map<String, Counter> versionCounters = new ConcurrentHashMap<>();

    /**
     * 策略命中计数器缓存
     */
    private final Map<String, Counter> strategyCounters = new ConcurrentHashMap<>();

    /**
     * 服务请求计数器缓存
     */
    private final Map<String, Counter> serviceCounters = new ConcurrentHashMap<>();

    /**
     * 路由决策计时器
     */
    private final Timer routingTimer;

    /**
     * 会话黏性命中计数
     */
    private final Counter sessionStickyHitCounter;

    /**
     * 会话黏性未命中计数
     */
    private final Counter sessionStickyMissCounter;

    /**
     * 灰度禁用时的请求计数
     */
    private final Counter grayDisabledCounter;

    /**
     * 版本未找到回退计数
     */
    private final Counter versionNotFoundCounter;

    /**
     * 活跃版本数量
     */
    private final AtomicLong activeVersionsGauge = new AtomicLong(0);

    public GrayMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 初始化固定的度量
        this.routingTimer = Timer.builder(METRIC_PREFIX + ".routing.time")
                .description("灰度路由决策耗时")
                .tags("type", "routing")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(meterRegistry);

        this.sessionStickyHitCounter = Counter.builder(METRIC_PREFIX + ".session.sticky.hit")
                .description("会话黏性命中次数")
                .register(meterRegistry);

        this.sessionStickyMissCounter = Counter.builder(METRIC_PREFIX + ".session.sticky.miss")
                .description("会话黏性未命中次数")
                .register(meterRegistry);

        this.grayDisabledCounter = Counter.builder(METRIC_PREFIX + ".disabled")
                .description("灰度禁用时的请求次数")
                .register(meterRegistry);

        this.versionNotFoundCounter = Counter.builder(METRIC_PREFIX + ".version.notfound")
                .description("目标版本未找到次数")
                .register(meterRegistry);

        // 注册活跃版本数量 gauge
        meterRegistry.gauge(METRIC_PREFIX + ".versions.active", activeVersionsGauge);

        log.info("灰度监控指标收集器初始化完成");
    }

    /**
     * 记录版本请求
     *
     * @param version 目标版本
     * @param service 服务名称
     */
    public void recordVersionRequest(String version, String service) {
        String key = version + ":" + service;
        Counter counter = versionCounters.computeIfAbsent(key, k -> Counter.builder(METRIC_PREFIX + ".version.requests")
                .description("各版本请求数量")
                .tags(Arrays.asList(
                        Tag.of("version", version),
                        Tag.of("service", service)))
                .register(meterRegistry));
        counter.increment();
    }

    /**
     * 记录策略命中
     *
     * @param strategy 策略名称（header/user/ip/weight）
     * @param service  服务名称
     */
    public void recordStrategyHit(String strategy, String service) {
        String key = strategy + ":" + service;
        Counter counter = strategyCounters.computeIfAbsent(key, k -> Counter.builder(METRIC_PREFIX + ".strategy.hit")
                .description("灰度策略命中次数")
                .tags(Arrays.asList(
                        Tag.of("strategy", strategy),
                        Tag.of("service", service)))
                .register(meterRegistry));
        counter.increment();
    }

    /**
     * 记录服务路由请求
     *
     * @param service    服务名称
     * @param instanceId 实例 ID
     */
    public void recordServiceRequest(String service, String instanceId) {
        String key = service + ":" + instanceId;
        Counter counter = serviceCounters.computeIfAbsent(key, k -> Counter.builder(METRIC_PREFIX + ".service.requests")
                .description("服务实例请求分布")
                .tags(Arrays.asList(
                        Tag.of("service", service),
                        Tag.of("instance", instanceId)))
                .register(meterRegistry));
        counter.increment();
    }

    /**
     * 记录路由决策耗时
     *
     * @param durationNanos 耗时（纳秒）
     */
    public void recordRoutingTime(long durationNanos) {
        routingTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * 记录路由决策耗时
     *
     * @param duration 耗时
     */
    public void recordRoutingTime(Duration duration) {
        routingTimer.record(duration);
    }

    /**
     * 记录会话黏性命中
     */
    public void recordSessionStickyHit() {
        sessionStickyHitCounter.increment();
    }

    /**
     * 记录会话黏性未命中
     */
    public void recordSessionStickyMiss() {
        sessionStickyMissCounter.increment();
    }

    /**
     * 记录灰度禁用请求
     */
    public void recordGrayDisabled() {
        grayDisabledCounter.increment();
    }

    /**
     * 记录版本未找到
     *
     * @param version 目标版本
     */
    public void recordVersionNotFound(String version) {
        versionNotFoundCounter.increment();
        log.debug("灰度版本未找到: {}", version);
    }

    /**
     * 更新活跃版本数量
     *
     * @param count 版本数量
     */
    public void updateActiveVersions(long count) {
        activeVersionsGauge.set(count);
    }

    /**
     * 获取版本请求统计
     *
     * @param version 版本号
     * @param service 服务名称
     * @return 请求数量
     */
    public double getVersionRequestCount(String version, String service) {
        String key = version + ":" + service;
        Counter counter = versionCounters.get(key);
        return counter != null ? counter.count() : 0;
    }

    /**
     * 获取策略命中统计
     *
     * @param strategy 策略名称
     * @param service  服务名称
     * @return 命中数量
     */
    public double getStrategyHitCount(String strategy, String service) {
        String key = strategy + ":" + service;
        Counter counter = strategyCounters.get(key);
        return counter != null ? counter.count() : 0;
    }

    /**
     * 获取路由决策平均耗时（毫秒）
     *
     * @return 平均耗时
     */
    public double getAverageRoutingTime() {
        return routingTimer.mean(TimeUnit.MILLISECONDS);
    }

    /**
     * 获取路由决策 P99 耗时（毫秒）
     *
     * @return P99 耗时
     */
    public double getP99RoutingTime() {
        return routingTimer.percentile(0.99, TimeUnit.MILLISECONDS);
    }

    /**
     * 获取会话黏性命中率
     *
     * @return 命中率（0-1）
     */
    public double getSessionStickyHitRate() {
        double hit = sessionStickyHitCounter.count();
        double miss = sessionStickyMissCounter.count();
        double total = hit + miss;
        return total > 0 ? hit / total : 0;
    }
}
