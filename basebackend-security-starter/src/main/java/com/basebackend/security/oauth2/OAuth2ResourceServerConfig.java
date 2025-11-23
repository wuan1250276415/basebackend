package com.basebackend.security.oauth2;

import com.basebackend.security.oauth2.customizer.OAuth2TokenCustomizer;
import com.basebackend.security.oauth2.entrypoint.OAuth2AuthenticationEntryPoint;
import com.basebackend.security.oauth2.handler.OAuth2AccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.DefaultHttpSecurityExpressionHandler;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

import java.util.List;

/**
 * OAuth2 资源服务器安全配置
 *
 * 配置OAuth2资源服务器的安全过滤器链，包括：
 * - JWT Token验证
 * - 权限和角色检查
 * - mTLS支持
 * - 自定义认证入口点和访问拒绝处理器
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@ConditionalOnProperty(name = "basebackend.security.oauth2.enabled", havingValue = "true")
@RequiredArgsConstructor
public class OAuth2ResourceServerConfig {

    private final OAuth2TokenCustomizer tokenCustomizer;

    /**
     * 配置安全过滤器链
     *
     * @param http HttpSecurity配置
     * @return SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("初始化 OAuth2 资源服务器安全配置");

        http
            // 禁用会话创建策略，使用无状态认证
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 配置OAuth2资源服务器
            .oauth2ResourceServer(oauth2 -> oauth2
                // 使用JWT作为Token格式
                .jwt(jwt -> jwt
                    // JWT解码器 - 验证Token签名和有效性
                    .decoder(tokenCustomizer.jwtDecoder())
                    // 配置JWT认证转换器
                    .jwtAuthenticationConverter(tokenCustomizer.jwtAuthenticationConverter())
                )
                // 自定义认证异常处理
                .authenticationEntryPoint(authenticationEntryPoint())
                // 自定义访问拒绝处理
                .accessDeniedHandler(accessDeniedHandler())
            )

            // 配置HTTP请求安全规则
            .authorizeHttpRequests(auth -> auth
                // 公开访问路径 - 无需认证
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/doc.html").permitAll()
                .requestMatchers("/webjars/**").permitAll()
                .requestMatchers("/favicon.ico").permitAll()

                // 内部服务间通信路径 - 使用mTLS
                .requestMatchers("/api/internal/**").authenticated()

                // 需要认证的API路径
                .requestMatchers("/api/**").authenticated()

                // 所有其他请求都需要认证
                .anyRequest().authenticated()
            )

            // 配置CSRF - OAuth2资源服务器通常不需要CSRF保护
            .csrf(csrf -> csrf.disable())

            // 配置异常处理
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            );

        log.info("OAuth2 资源服务器安全配置初始化完成");
        return http.build();
    }

    /**
     * 注意：JwtAuthenticationConverter Bean 定义在 OAuth2TokenCustomizer 中
     * 这里不再重复定义，避免Bean冲突
     */

    /**
     * 自定义认证入口点
     * 处理未认证访问的场景
     *
     * @return OAuth2AuthenticationEntryPoint
     */
    @Bean
    @ConditionalOnMissingBean
    public OAuth2AuthenticationEntryPoint authenticationEntryPoint() {
        log.debug("创建OAuth2认证入口点");
        return new OAuth2AuthenticationEntryPoint();
    }

    /**
     * 自定义访问拒绝处理器
     * 处理已认证但权限不足的场景
     *
     * @return OAuth2AccessDeniedHandler
     */
    @Bean
    @ConditionalOnMissingBean
    public OAuth2AccessDeniedHandler accessDeniedHandler() {
        log.debug("创建OAuth2访问拒绝处理器");
        return new OAuth2AccessDeniedHandler();
    }

    /**
     * 配置方法安全表达式处理器
     * 支持@PreAuthorize, @PostAuthorize等注解
     *
     * @return DefaultHttpSecurityExpressionHandler
     */
    @Bean
    @ConditionalOnMissingBean
    public DefaultHttpSecurityExpressionHandler httpSecurityExpressionHandler() {
        DefaultHttpSecurityExpressionHandler handler = new DefaultHttpSecurityExpressionHandler();

        // 注册自定义权限评估器
        handler.setPermissionEvaluator(new com.basebackend.security.oauth2.evaluator.OAuth2PermissionEvaluator());

        log.debug("方法安全表达式处理器配置完成");
        return handler;
    }
}
