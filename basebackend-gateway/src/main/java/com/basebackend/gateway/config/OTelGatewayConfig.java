package com.basebackend.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry Gateway 配置
 * <p>
 * 仅在 OpenTelemetry API 存在时激活。
 * 使用反射避免编译时依赖 OpenTelemetry API。
 * </p>
 */
@Configuration
@ConditionalOnClass(name = "io.opentelemetry.api.trace.Span")
public class OTelGatewayConfig {

    @Bean
    public Object w3cTextMapPropagator() throws Exception {
        // 使用反射获取 GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
        Class<?> globalOTelClass = Class.forName("io.opentelemetry.api.GlobalOpenTelemetry");
        Object propagators = globalOTelClass.getMethod("getPropagators").invoke(null);
        return propagators.getClass().getMethod("getTextMapPropagator").invoke(propagators);
    }
}
