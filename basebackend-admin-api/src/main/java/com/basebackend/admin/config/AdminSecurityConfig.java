package com.basebackend.admin.config;

import com.basebackend.admin.filter.JwtAuthenticationFilter;
import com.basebackend.web.filter.CsrfCookieFilter;
import com.basebackend.web.filter.OriginValidationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * ��̨����API��ȫ����
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class AdminSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CsrfCookieFilter csrfCookieFilter;
    private final OriginValidationFilter originValidationFilter;

    /**
     * ���������
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * ��ȫ������������
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        MvcRequestMatcher.Builder mvc = new MvcRequestMatcher.Builder(introspector).servletPath("/");
        RequestMatcher actuatorMatcher = new AntPathRequestMatcher("/actuator/**");
        RequestMatcher[] csrfIgnoredMatchers = new RequestMatcher[]{
                mvc.pattern("/api/admin/auth/**"),
                mvc.pattern("/api/public/**"),
                actuatorMatcher,
                mvc.pattern("/swagger-ui/**"),
                mvc.pattern("/v3/api-docs/**"),
                mvc.pattern("/doc.html"),
                mvc.pattern("/webjars/**"),
                mvc.pattern("/favicon.ico")
        };
        RequestMatcher[] publicMatchers = new RequestMatcher[]{
                mvc.pattern("/api/admin/auth/**"),
                mvc.pattern("/api/admin/users"),
                mvc.pattern("/api/public/**"),
                actuatorMatcher,
                mvc.pattern("/swagger-ui/**"),
                mvc.pattern("/v3/api-docs/**"),
                mvc.pattern("/doc.html"),
                mvc.pattern("/webjars/**"),
                mvc.pattern("/favicon.ico")
        };

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers(csrfIgnoredMatchers)
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .headers(headers -> {
                    headers.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; img-src 'self' data:; object-src 'none'; frame-ancestors 'none'; frame-src 'none'; form-action 'self'; base-uri 'self';"));
                    headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN));
                    headers.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).preload(true));
                    headers.frameOptions(frame -> frame.deny());
                    headers.permissionsPolicy(policy -> policy.policy("geolocation=(), microphone=(), camera=()"));
                })
                // ����������Ȩ
                .authorizeHttpRequests(auth -> auth
                        // �����ӿ�
                        .requestMatchers(publicMatchers).permitAll()
                        // ��������������Ҫ��֤
                        .anyRequest().authenticated()
                )
                // ����JWT������
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(csrfCookieFilter, BasicAuthenticationFilter.class)
                .addFilterAfter(originValidationFilter, CsrfFilter.class);

        return http.build();
    }
}
