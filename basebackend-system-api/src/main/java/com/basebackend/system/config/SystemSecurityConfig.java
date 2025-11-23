//package com.basebackend.system.config;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
//
//import static org.springframework.security.config.Customizer.withDefaults;
//
///**
// * 系统服务安全配置
// *
// * @author Claude Code (浮浮酱)
// * @since 2025-11-26
// */
//@Slf4j
//@Configuration
//@EnableWebSecurity
//public class SystemSecurityConfig {
//
//    /**
//     * 配置系统服务特定的公开接口
//     * 如果 basebackend-security 已配置，则此 bean 不会被创建
//     */
//    @Bean
//    @ConditionalOnMissingBean(SecurityFilterChain.class)
//    public SecurityFilterChain systemSecurityFilterChain(HttpSecurity http) throws Exception {
//        log.info("初始化系统服务安全配置");
//
//        http
//                // 授权请求配置
//                .authorizeHttpRequests(auth -> auth
//                        // system-api 的公开接口
//                        .requestMatchers(
//                                new AntPathRequestMatcher("/api/system/depts/tree"),
//                                new AntPathRequestMatcher("/api/system/depts/by-name"),
//                                new AntPathRequestMatcher("/api/system/depts/by-code"),
//                                new AntPathRequestMatcher("/api/system/depts/batch"),
//                                new AntPathRequestMatcher("/api/system/depts/by-parent"),
//                                new AntPathRequestMatcher("/api/user/application/enabled"),
//                                new AntPathRequestMatcher("/api/user/application/code/**"),
//                                new AntPathRequestMatcher("/api/dicts/**"),
//                                new AntPathRequestMatcher("/actuator/health"),
//                                new AntPathRequestMatcher("/actuator/**"),
//                                new AntPathRequestMatcher("/v3/api-docs/**"),
//                                new AntPathRequestMatcher("/doc.html"),
//                                new AntPathRequestMatcher("/swagger-ui/**"),
//                                new AntPathRequestMatcher("/webjars/**")
//                        ).permitAll()
//                        // 其他所有请求需要认证
//                        .anyRequest().authenticated()
//                );
//
//        return http.build();
//    }
//}
