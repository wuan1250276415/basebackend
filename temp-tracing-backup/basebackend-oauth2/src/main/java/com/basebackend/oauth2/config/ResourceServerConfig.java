package com.basebackend.oauth2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth2.0资源服务器配置
 * 用于验证其他微服务的JWT令牌
 */
@Configuration
public class ResourceServerConfig {

    /**
     * JWT权限转换器
     * 将JWT中的权限信息转换为Spring Security的权限对象
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthorityClaimName("roles");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return authenticationConverter;
    }

    /**
     * 资源服务器安全过滤器链
     * 用于保护API端点，只允许持有有效JWT令牌的用户访问
     */
    @Bean
    public SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // 公开端点
                        .requestMatchers("/public/**", "/actuator/health").permitAll()
                        // OAuth2.0端点
                        .requestMatchers("/oauth2/**").permitAll()
                        // 静态资源
                        .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**").permitAll()
                        // 文档端点
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 需要认证的端点
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())
                ));

        return http.build();
    }

    /**
     * JWT认证提供者
     * 用于验证JWT令牌
     */
    @Bean
    public JwtAuthenticationProvider jwtAuthenticationProvider() {
        return new JwtAuthenticationProvider(jwtAuthenticationConverter());
    }
}
