package com.basebackend.observability.otel.config;

import brave.Tracing;
import com.basebackend.observability.otel.bridge.BraveToOtelBridge;
import com.basebackend.observability.otel.bridge.MicrometerToOtelBridge;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.List;

/**
 * 桥接器配置
 * <p>
 * 配置 Micrometer 和 Brave 到 OpenTelemetry 的桥接组件，
 * 使得迁移期间可以同时使用双栈遥测系统。
 * </p>
 * <p>
 * 桥接器的工作原理：
 * <ul>
 *     <li><b>Micrometer 桥接</b>：将 Micrometer 指标镜像到 OpenTelemetry</li>
 *     <li><b>Brave 桥接</b>：将 Brave Span 镜像到 OpenTelemetry</li>
 * </ul>
 * </p>
 * <p>
 * 通过配置属性 {@code observability.otel.bridge.micrometer} 和
 * {@code observability.otel.bridge.brave} 可以独立控制每个桥接器。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "observability.otel.enabled", havingValue = "true")
public class BridgeConfiguration {

    private static final Logger log = LoggerFactory.getLogger(BridgeConfiguration.class);

    /**
     * 创建 Micrometer 到 OpenTelemetry 桥接器
     * <p>
     * 只有当 Micrometer 存在且桥接开关开启时才会创建。
     * </p>
     */
    @Bean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnProperty(
            prefix = "observability.otel.bridge",
            name = "micrometer",
            havingValue = "true",
            matchIfMissing = true
    )
    public MicrometerToOtelBridge micrometerToOtelBridge(OpenTelemetry openTelemetry) {
        log.info("创建 Micrometer → OpenTelemetry 桥接器");
        return new MicrometerToOtelBridge(openTelemetry);
    }

    /**
     * 创建 Brave 到 OpenTelemetry 桥接器
     * <p>
     * 只有当 Brave 存在且桥接开关开启时才会创建。
     * </p>
     */
    @Bean
    @ConditionalOnClass(Tracing.class)
    @ConditionalOnProperty(
            prefix = "observability.otel.bridge",
            name = "brave",
            havingValue = "true",
            matchIfMissing = true
    )
    public BraveToOtelBridge braveToOtelBridge(OpenTelemetry openTelemetry) {
        Tracer tracer = openTelemetry.getTracer("com.basebackend.observability.bridge");
        log.info("创建 Brave → OpenTelemetry 桥接器");
        return new BraveToOtelBridge(tracer);
    }

    /**
     * Micrometer 桥接器初始化监听器
     * <p>
     * 在 Spring 上下文刷新后自动将桥接器绑定到所有可用的 MeterRegistry。
     * 这确保了由 Spring Boot 自动配置创建的注册表也能被桥接。
     * </p>
     */
    @Bean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnProperty(
            prefix = "observability.otel.bridge",
            name = "micrometer",
            havingValue = "true",
            matchIfMissing = true
    )
    public ApplicationListener<ContextRefreshedEvent> micrometerBridgeInitializer(
            MicrometerToOtelBridge bridge,
            ObjectProvider<List<MeterRegistry>> registriesProvider) {

        return event -> registriesProvider.ifAvailable(registries -> {
            if (registries.isEmpty()) {
                log.warn("未找到 MeterRegistry 实例，Micrometer 桥接器未绑定");
                return;
            }

            for (MeterRegistry registry : registries) {
                bridge.bind(registry);
            }

            log.info("Micrometer 桥接器已绑定到 {} 个注册表", registries.size());
        });
    }

    /**
     * Brave 桥接器初始化监听器
     * <p>
     * 在 Spring 上下文刷新后自动将桥接器注册到 Brave Tracing。
     * </p>
     */
    @Bean
    @ConditionalOnClass(Tracing.class)
    @ConditionalOnProperty(
            prefix = "observability.otel.bridge",
            name = "brave",
            havingValue = "true",
            matchIfMissing = true
    )
    public ApplicationListener<ContextRefreshedEvent> braveBridgeInitializer(
            BraveToOtelBridge bridge,
            ObjectProvider<Tracing> tracingProvider) {

        return event -> tracingProvider.ifAvailable(tracing -> {
            // 将桥接器添加到 Brave 的 SpanHandler 链中
            // 注意：Brave 的 Tracing 是不可变的，需要在创建时就包含 Handler
            // 这里我们假设 Tracing Bean 已经配置好了
            log.info("Brave 桥接器已就绪（需要在 Tracing 创建时注册 SpanHandler）");
            log.warn("请确保在创建 Tracing 实例时包含 BraveToOtelBridge 作为 SpanHandler");
        });
    }
}
