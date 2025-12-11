package com.basebackend.security.starter;

import com.basebackend.security.aspect.DataScopeAspect;
import com.basebackend.security.aspect.PermissionAspect;
import com.basebackend.security.filter.CsrfCookieFilter;
import com.basebackend.security.filter.InternalServiceAuthFilter;
import com.basebackend.security.filter.JwtAuthenticationFilter;
import com.basebackend.security.filter.OriginValidationFilter;
import com.basebackend.security.matcher.InternalServiceRequestMatcher;
import com.basebackend.security.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;

/**
 * 安全自动配置
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
@EnableAspectJAutoProxy
@RequiredArgsConstructor
public class SecurityAutoConfiguration {

    private final SecurityProperties securityProperties;

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 安全配置 Bean
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CsrfCookieFilter csrfCookieFilter,
            OriginValidationFilter originValidationFilter,
            InternalServiceAuthFilter internalServiceAuthFilter) throws Exception {

        log.info("初始化 BaseBackend 安全配置 - enabled: {}", securityProperties.isEnabled());

        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                new InternalServiceRequestMatcher(), // 忽略内部服务调用
                                request -> request.getRequestURI().startsWith("/api/auth/"),
                                request -> request.getRequestURI().startsWith("/api/user/auth/"),
                                request -> request.getRequestURI().startsWith("/api/public/"),
                                request -> request.getRequestURI().startsWith("/actuator/"),
                                request -> request.getRequestURI().startsWith("/v3/api-docs/"),
                                request -> request.getRequestURI().equals("/doc.html"),
                                request -> request.getRequestURI().startsWith("/swagger-ui/"),
                                request -> request.getRequestURI().startsWith("/webjars/"),
                                request -> request.getRequestURI().equals("/favicon.ico"),
                                request -> request.getRequestURI().startsWith("/druid/")))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/user/auth/**",
                                "/api/public/**",
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/doc.html",
                                "/swagger-ui/**",
                                "/webjars/**",
                                "/favicon.ico",
                                "/druid/**")
                        .permitAll()
                        .anyRequest().authenticated())
                // 内部服务认证过滤器应在 JWT 过滤器之前
                .addFilterBefore(internalServiceAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, InternalServiceAuthFilter.class)
                .addFilterAfter(csrfCookieFilter, BasicAuthenticationFilter.class)
                .addFilterAfter(originValidationFilter, CsrfFilter.class);

        return http.build();
    }

    /**
     * 权限切面
     */
    @Bean
    @ConditionalOnProperty(name = "basebackend.security.permission.enabled", havingValue = "true")
    public PermissionAspect permissionAspect(PermissionService permissionService) {
        return new PermissionAspect(permissionService);
    }

    /**
     * 数据权限切面
     */
    @Bean
    @ConditionalOnProperty(name = "basebackend.security.data-scope.enabled", havingValue = "true")
    public DataScopeAspect dataScopeAspect() {
        return new DataScopeAspect();
    }

    /**
     * 内部服务认证过滤器
     */
    @Bean
    @ConditionalOnMissingBean
    public InternalServiceAuthFilter internalServiceAuthFilter() {
        return new InternalServiceAuthFilter();
    }

    /**
     * 安全条件检查
     */
    @Bean
    @ConditionalOnMissingBean
    public SecurityConditionChecker securityConditionChecker() {
        return new SecurityConditionChecker();
    }

    /**
     * 安全条件检查器
     */
    public static class SecurityConditionChecker {
        public SecurityConditionChecker() {
            log.info("✅ BaseBackend Security Starter 已加载");
            log.info("   - 权限注解: @RequiresPermission, @RequiresRole");
            log.info("   - 数据权限: @DataScope");
            log.info("   - JWT 认证: 已启用");
            log.info("   - 内部服务认证: 已启用");
        }
    }
}
