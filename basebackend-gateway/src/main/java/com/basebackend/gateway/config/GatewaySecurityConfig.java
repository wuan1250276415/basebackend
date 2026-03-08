package com.basebackend.gateway.config;

import com.basebackend.common.security.SecretManager;
import com.basebackend.common.security.SecretManagerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;

/**
 * Gateway Security配置
 * 为Gateway提供必要的安全组件，但不使用Servlet相关的Filter
 */
@Configuration
@EnableConfigurationProperties(SecretManagerProperties.class)
public class GatewaySecurityConfig {

    /**
     * 为JwtUtil提供SecretManager
     * Gateway只需要SecretManager来读取JWT密钥
     */
    @Bean
    public SecretManager secretManager(ConfigurableEnvironment environment,
                                      SecretManagerProperties properties) {
        return new SecretManager(environment, properties);
    }

    /**
     * 管理面安全链路
     * <p>
     * 仅拦截本地管理端点（actuator 与 sentinel-test），其余网关业务流量仍由网关过滤器链处理。
     * 默认拒绝，仅放行 gateway.security.actuator-whitelist 中显式声明的端点。
     * </p>
     */
    @Bean
    @Order(0)
    public SecurityWebFilterChain managementSecurityWebFilterChain(ServerHttpSecurity http,
                                                                   GatewaySecurityProperties securityProperties) {
        String[] actuatorWhitelist = securityProperties.getActuatorWhitelist() == null
                ? new String[0]
                : securityProperties.getActuatorWhitelist().toArray(String[]::new);

        OrServerWebExchangeMatcher managementMatcher = new OrServerWebExchangeMatcher(
                new PathPatternParserServerWebExchangeMatcher("/actuator/**"),
                new PathPatternParserServerWebExchangeMatcher("/sentinel-test/**"));

        return http.securityMatcher(managementMatcher)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .authorizeExchange(exchange -> {
                    if (actuatorWhitelist.length > 0) {
                        exchange.pathMatchers(actuatorWhitelist).permitAll();
                    }
                    exchange.pathMatchers(HttpMethod.GET, "/sentinel-test/health").permitAll();
                    exchange.anyExchange().denyAll();
                })
                .build();
    }
}
