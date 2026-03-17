package com.basebackend.security.config;

import com.basebackend.security.filter.AuthenticationRateLimitFilter;
import com.basebackend.security.filter.CsrfCookieFilter;
import com.basebackend.security.filter.OriginValidationFilter;
import com.basebackend.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Spring Security 配置
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CsrfCookieFilter csrfCookieFilter;
    private final OriginValidationFilter originValidationFilter;

    /**
     * 可选：认证速率限制过滤器，仅在 RateLimitConfiguration 生效时注入
     */
    @Autowired(required = false)
    private AuthenticationRateLimitFilter authenticationRateLimitFilter;

    /**
     * 公开端点路径 — 无需认证即可访问（同时豁免 CSRF 保护）
     */
    private static final String[] PUBLIC_PATHS = {
            "/api/auth/**",
            "/api/user/auth/**",
            "/api/public/**",
            "/api/files/**",
            "/api/auth/wechat/**",
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/doc.html",
            "/webjars/**",
            "/favicon.ico",
            "/api/notifications/stream"
    };

    /**
     * 仅豁免 CSRF 保护但仍需认证的路径（如 API 文档静态资源）
     */
    private static final String[] CSRF_ONLY_EXEMPT_PATHS = {
            "/druid/**"
    };

    /**
     * 安全过滤器链配置
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        http
                .csrf(csrf -> {
                    csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler);

                    // 公开路径豁免 CSRF
                    for (String path : PUBLIC_PATHS) {
                        csrf.ignoringRequestMatchers(PathPatternRequestMatcher.pathPattern(path));
                    }
                    // 仅 CSRF 豁免路径（如 Druid 控制台使用自身 CSRF 机制）
                    for (String path : CSRF_ONLY_EXEMPT_PATHS) {
                        csrf.ignoringRequestMatchers(PathPatternRequestMatcher.pathPattern(path));
                    }
                })
                // 禁用表单登录
                .formLogin(AbstractHttpConfigurer::disable)
                // 禁用HTTP基本认证
                .httpBasic(AbstractHttpConfigurer::disable)
                // 设置会话管理策略为无状态
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 安全响应头
                .headers(headers -> {
                    headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'self'; "
                            + "script-src 'self'; "
                            + "style-src 'self' 'unsafe-inline'; "
                            + "img-src 'self' data:; "
                            + "object-src 'none'; "
                            + "frame-ancestors 'none'; "
                            + "frame-src 'none'; "
                            + "form-action 'self'; "
                            + "base-uri 'self';"));
                    headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN));
                    headers.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).preload(true));
                    headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny);
                    headers.permissionsPolicy(policy -> policy.policy("geolocation=(), microphone=(), camera=()"));
                })
                // 配置请求授权
                .authorizeHttpRequests(auth -> {
                    // 公开接口
                    for (String path : PUBLIC_PATHS) {
                        auth.requestMatchers(PathPatternRequestMatcher.pathPattern(path)).permitAll();
                    }
                    // [P0-1] 已移除 X-Internal-Call 请求头信任机制。
                    // 内部 Feign 调用应通过 Gateway 层剥离外部伪造头、
                    // 或使用 mTLS / 内网网段校验 / 签名机制来验证调用来源。
                    // [P1-1] Druid 面板不再 permitAll，需认证后访问。
                    auth.anyRequest().authenticated();
                })
                // 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(csrfCookieFilter, BasicAuthenticationFilter.class)
                .addFilterAfter(originValidationFilter, CsrfFilter.class);

        // 速率限制过滤器置于 JWT 过滤器之前，尽早拦截暴力请求
        if (authenticationRateLimitFilter != null) {
            http.addFilterBefore(authenticationRateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }
}
