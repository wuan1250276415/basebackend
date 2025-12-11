package com.basebackend.nacos.metrics;

import com.basebackend.nacos.constants.NacosConstants;
import com.basebackend.nacos.event.ConfigChangeEvent;
import com.basebackend.nacos.event.GrayReleaseHistoryEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Nacos监控指标
 * <p>
 * 提供Nacos配置和服务发现的监控指标，支持与Prometheus/Grafana集成。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnClass(MeterRegistry.class)
public class NacosMetrics {

    private final MeterRegistry registry;

    public NacosMetrics(MeterRegistry registry) {
        this.registry = registry;
        log.info("NacosMetrics initialized");
    }

    // ========== 配置获取指标 ==========

    /**
     * 记录配置获取成功
     *
     * @param dataId 数据ID
     * @param group  分组
     */
    public void recordConfigGetSuccess(String dataId, String group) {
        Counter.builder(NacosConstants.METRIC_CONFIG_GET)
                .tag("dataId", dataId)
                .tag("group", group)
                .tag("status", "success")
                .register(registry)
                .increment();
    }

    /**
     * 记录配置获取失败
     *
     * @param dataId    数据ID
     * @param group     分组
     * @param errorType 错误类型
     */
    public void recordConfigGetFailure(String dataId, String group, String errorType) {
        Counter.builder(NacosConstants.METRIC_CONFIG_GET)
                .tag("dataId", dataId)
                .tag("group", group)
                .tag("status", "failure")
                .tag("error", errorType)
                .register(registry)
                .increment();
    }

    /**
     * 记录配置获取耗时
     *
     * @param dataId    数据ID
     * @param group     分组
     * @param latencyMs 耗时（毫秒）
     */
    public void recordConfigGetLatency(String dataId, String group, long latencyMs) {
        Timer.builder(NacosConstants.METRIC_CONFIG_GET + ".latency")
                .tag("dataId", dataId)
                .tag("group", group)
                .register(registry)
                .record(latencyMs, TimeUnit.MILLISECONDS);
    }

    // ========== 配置发布指标 ==========

    /**
     * 记录配置发布成功
     *
     * @param dataId 数据ID
     * @param group  分组
     */
    public void recordConfigPublishSuccess(String dataId, String group) {
        Counter.builder(NacosConstants.METRIC_CONFIG_PUBLISH)
                .tag("dataId", dataId)
                .tag("group", group)
                .tag("status", "success")
                .register(registry)
                .increment();
    }

    /**
     * 记录配置发布失败
     *
     * @param dataId    数据ID
     * @param group     分组
     * @param errorType 错误类型
     */
    public void recordConfigPublishFailure(String dataId, String group, String errorType) {
        Counter.builder(NacosConstants.METRIC_CONFIG_PUBLISH)
                .tag("dataId", dataId)
                .tag("group", group)
                .tag("status", "failure")
                .tag("error", errorType)
                .register(registry)
                .increment();
    }

    // ========== 配置变更事件监听 ==========

    /**
     * 监听配置变更事件
     */
    @EventListener
    public void handleConfigChange(ConfigChangeEvent event) {
        Counter.builder(NacosConstants.METRIC_CONFIG_CHANGE)
                .tag("dataId", event.getDataId())
                .tag("group", event.getGroup())
                .register(registry)
                .increment();

        log.debug("Config change recorded: dataId={}, group={}",
                event.getDataId(), event.getGroup());
    }

    // ========== 灰度发布指标 ==========

    /**
     * 监听灰度发布事件
     */
    @EventListener
    public void handleGrayRelease(GrayReleaseHistoryEvent event) {
        if (event.getHistory() != null) {
            Counter.builder(NacosConstants.METRIC_GRAY_RELEASE)
                    .tag("strategy", String.valueOf(event.getHistory().getStrategyType()))
                    .tag("result", event.getHistory().getResult())
                    .register(registry)
                    .increment();
        }
    }

    /**
     * 记录灰度发布操作
     *
     * @param strategyType 策略类型
     * @param success      是否成功
     */
    public void recordGrayRelease(String strategyType, boolean success) {
        Counter.builder(NacosConstants.METRIC_GRAY_RELEASE)
                .tag("strategy", strategyType)
                .tag("status", success ? "success" : "failure")
                .register(registry)
                .increment();
    }

    // ========== 重试指标 ==========

    /**
     * 记录重试
     *
     * @param operation  操作类型
     * @param retryCount 重试次数
     */
    public void recordRetry(String operation, int retryCount) {
        Counter.builder(NacosConstants.METRIC_PREFIX + ".retry")
                .tag("operation", operation)
                .tag("count", String.valueOf(retryCount))
                .register(registry)
                .increment();
    }

    // ========== 服务发现指标 ==========

    /**
     * 记录服务发现操作
     *
     * @param serviceName 服务名
     * @param success     是否成功
     */
    public void recordServiceDiscovery(String serviceName, boolean success) {
        Counter.builder(NacosConstants.METRIC_SERVICE_DISCOVERY)
                .tag("service", serviceName)
                .tag("status", success ? "success" : "failure")
                .register(registry)
                .increment();
    }

    /**
     * 记录服务实例数量
     *
     * @param serviceName   服务名
     * @param instanceCount 实例数量
     */
    public void recordServiceInstances(String serviceName, int instanceCount) {
        registry.gauge(NacosConstants.METRIC_SERVICE_DISCOVERY + ".instances",
                java.util.Collections.singletonList(io.micrometer.core.instrument.Tag.of("service", serviceName)),
                instanceCount);
    }
}
