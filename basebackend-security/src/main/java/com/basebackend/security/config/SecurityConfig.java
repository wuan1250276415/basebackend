package com.basebackend.security.config;

import com.basebackend.security.filter.CsrfCookieFilter;
import com.basebackend.security.filter.OriginValidationFilter;
import com.basebackend.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
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
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
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
     * 安全过滤器链配置
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers(
                                PathPatternRequestMatcher.pathPattern("/api/auth/**"),
                                PathPatternRequestMatcher.pathPattern("/api/user/**"),
                                PathPatternRequestMatcher.pathPattern("/api/public/**"),
                                PathPatternRequestMatcher.pathPattern("/actuator/**"),
                                PathPatternRequestMatcher.pathPattern("/v3/api-docs/**"),
                                PathPatternRequestMatcher.pathPattern("/doc.html"),
                                PathPatternRequestMatcher.pathPattern("/api/files/**"),
                                PathPatternRequestMatcher.pathPattern("/api/auth/wechat/**"),
                                PathPatternRequestMatcher.pathPattern("/swagger-ui/**"),
                                PathPatternRequestMatcher.pathPattern("/druid/**"),
                                PathPatternRequestMatcher.pathPattern("/api/notifications/stream")
                        )
                )
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
                    headers.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; img-src 'self' data:; object-src 'none'; frame-ancestors 'none'; frame-src 'none'; form-action 'self'; base-uri 'self';"));
                    headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN));
                    headers.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).preload(true));
                    headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny);
                    headers.permissionsPolicy(policy -> policy.policy("geolocation=(), microphone=(), camera=()"));
                })
                // 配置请求授权
                .authorizeHttpRequests(auth -> auth
                        // 公开接口
                        .requestMatchers(
                                PathPatternRequestMatcher.pathPattern("/api/auth/**"),
                                PathPatternRequestMatcher.pathPattern("/api/user/auth/**"),
                                PathPatternRequestMatcher.pathPattern("/api/public/**"),
                                PathPatternRequestMatcher.pathPattern("/api/files/**"),
                                PathPatternRequestMatcher.pathPattern("/api/auth/wechat/**"),
                                PathPatternRequestMatcher.pathPattern("/actuator/**"),
                                PathPatternRequestMatcher.pathPattern("/swagger-ui/**"),
                                PathPatternRequestMatcher.pathPattern("/v3/api-docs/**"),
                                PathPatternRequestMatcher.pathPattern("/doc.html"),
                                PathPatternRequestMatcher.pathPattern("/webjars/**"),
                                PathPatternRequestMatcher.pathPattern("/favicon.ico"),
                                PathPatternRequestMatcher.pathPattern("/druid/**"),
                                PathPatternRequestMatcher.pathPattern("/api/notifications/stream")
                        ).permitAll()
                        // 内部 Feign 调用（由 FeignAuthRequestInterceptor 注入标记）
                        .requestMatchers(new RequestHeaderRequestMatcher("X-Internal-Call", "true")).permitAll()
                        // 其他所有请求需要认证
                        .anyRequest().authenticated()
                )
                // 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(csrfCookieFilter, BasicAuthenticationFilter.class)
                .addFilterAfter(originValidationFilter, CsrfFilter.class);

        return http.build();
    }
}
