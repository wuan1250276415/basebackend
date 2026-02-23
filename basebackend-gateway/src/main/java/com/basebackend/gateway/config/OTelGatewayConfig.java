package com.basebackend.gateway.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "io.opentelemetry.api.trace.Span")
public class OTelGatewayConfig {

    @Bean
    public TextMapPropagator w3cTextMapPropagator() {
        return GlobalOpenTelemetry.getPropagators().getTextMapPropagator();
    }
}
