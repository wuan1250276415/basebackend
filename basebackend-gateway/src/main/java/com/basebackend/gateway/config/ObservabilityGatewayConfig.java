package com.basebackend.gateway.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway observability integration.
 * Ensures only WebFlux-compatible observability components are loaded.
 * Servlet-based tracing (HttpTracingConfiguration) is disabled via application.yml properties.
 */
@Slf4j
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class ObservabilityGatewayConfig {

    @PostConstruct
    public void init() {
        log.info("Gateway observability integration active (reactive mode)");
    }
}
