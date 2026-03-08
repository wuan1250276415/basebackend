/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.micrometer.core.instrument.Counter
 *  io.micrometer.core.instrument.MeterRegistry
 *  io.micrometer.core.instrument.Tag
 *  io.micrometer.core.instrument.Timer
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnClass
 *  org.springframework.context.event.EventListener
 *  org.springframework.stereotype.Component
 */
package com.basebackend.nacos.metrics;

import com.basebackend.nacos.event.ConfigChangeEvent;
import com.basebackend.nacos.event.GrayReleaseHistoryEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnClass(value={MeterRegistry.class})
public class NacosMetrics {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(NacosMetrics.class);
    private final MeterRegistry registry;

    public NacosMetrics(MeterRegistry registry) {
        this.registry = registry;
        log.info("NacosMetrics initialized");
    }

    public void recordConfigGetSuccess(String dataId, String group) {
        Counter.builder((String)"nacos.config.get").tag("dataId", dataId).tag("group", group).tag("status", "success").register(this.registry).increment();
    }

    public void recordConfigGetFailure(String dataId, String group, String errorType) {
        Counter.builder((String)"nacos.config.get").tag("dataId", dataId).tag("group", group).tag("status", "failure").tag("error", errorType).register(this.registry).increment();
    }

    public void recordConfigGetLatency(String dataId, String group, long latencyMs) {
        Timer.builder((String)"nacos.config.get.latency").tag("dataId", dataId).tag("group", group).register(this.registry).record(latencyMs, TimeUnit.MILLISECONDS);
    }

    public void recordConfigPublishSuccess(String dataId, String group) {
        Counter.builder((String)"nacos.config.publish").tag("dataId", dataId).tag("group", group).tag("status", "success").register(this.registry).increment();
    }

    public void recordConfigPublishFailure(String dataId, String group, String errorType) {
        Counter.builder((String)"nacos.config.publish").tag("dataId", dataId).tag("group", group).tag("status", "failure").tag("error", errorType).register(this.registry).increment();
    }

    @EventListener
    public void handleConfigChange(ConfigChangeEvent event) {
        Counter.builder((String)"nacos.config.change").tag("dataId", event.getDataId()).tag("group", event.getGroup()).register(this.registry).increment();
        log.debug("Config change recorded: dataId={}, group={}", (Object)event.getDataId(), (Object)event.getGroup());
    }

    @EventListener
    public void handleGrayRelease(GrayReleaseHistoryEvent event) {
        if (event.getHistory() != null) {
            Counter.builder((String)"nacos.gray.release").tag("strategy", String.valueOf(event.getHistory().getStrategyType())).tag("result", event.getHistory().getResult()).register(this.registry).increment();
        }
    }

    public void recordGrayRelease(String strategyType, boolean success) {
        Counter.builder((String)"nacos.gray.release").tag("strategy", strategyType).tag("status", success ? "success" : "failure").register(this.registry).increment();
    }

    public void recordRetry(String operation, int retryCount) {
        Counter.builder((String)"nacos.retry").tag("operation", operation).tag("count", String.valueOf(retryCount)).register(this.registry).increment();
    }

    public void recordServiceDiscovery(String serviceName, boolean success) {
        Counter.builder((String)"nacos.service.discovery").tag("service", serviceName).tag("status", success ? "success" : "failure").register(this.registry).increment();
    }

    public void recordServiceInstances(String serviceName, int instanceCount) {
        this.registry.gauge("nacos.service.discovery.instances", Collections.singletonList(Tag.of((String)"service", (String)serviceName)), (Number)instanceCount);
    }
}

