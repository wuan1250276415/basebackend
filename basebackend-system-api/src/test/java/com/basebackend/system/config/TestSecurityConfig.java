package com.basebackend.system.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 测试安全配置
 * <p>
 * 禁用CSRF和启用所有请求的授权，用于测试环境。
 * </p>
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
            .headers(headers -> headers.frameOptions().disable());
        return http.build();
    }
}
