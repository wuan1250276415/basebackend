package com.basebackend.security.config;

import com.basebackend.security.filter.JwtAuthenticationFilter;
import com.basebackend.security.filter.CsrfCookieFilter;
import com.basebackend.security.filter.OriginValidationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 安全配置测试
 * 测试Spring Security配置
 *
 * @author BaseBackend
 */
@DisplayName("SecurityConfig 安全配置测试")
class SecurityConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SecurityConfig.class, SecurityBaselineConfiguration.class))
        .withUserConfiguration(TestSecurityBeans.class);

    @Test
    @DisplayName("SecurityConfig应该被自动配置")
    void shouldAutoConfigureSecurityConfig() {
        // When
        contextRunner.run(context -> {
            // Then
            assertThat(context).hasSingleBean(SecurityConfig.class);
            assertThat(context).hasSingleBean(SecurityFilterChain.class);
        });
    }

    @Test
    @DisplayName("SecurityFilterChain应该被配置")
    void shouldConfigureSecurityFilterChain() {
        // When
        contextRunner.run(context -> {
            // Then
            SecurityFilterChain filterChain = context.getBean(SecurityFilterChain.class);
            assertThat(filterChain).isNotNull();

            // 验证基本配置
            SecurityConfig config = context.getBean(SecurityConfig.class);
            assertThat(config).isNotNull();
        });
    }

    @Test
    @DisplayName("应该支持自定义配置")
    void shouldSupportCustomConfiguration() {
        // Given
        @Configuration
        @EnableWebSecurity
        static class CustomSecurityConfig {
            @Bean
            public SecurityFilterChain customFilterChain(HttpSecurity http) throws Exception {
                return http
                    .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                    )
                    .csrf(csrf -> csrf.disable())
                    .build();
            }
        }

        // When
        contextRunner.withUserConfiguration(CustomSecurityConfig.class)
            .run(context -> {
                // Then - 应该至少有一个SecurityFilterChain bean
                assertThat(context).hasBean("customFilterChain");
            });
    }

    @Test
    @DisplayName("配置类不应该有重复的SecurityFilterChain Bean")
    void shouldNotHaveDuplicateSecurityFilterChain() {
        // When
        contextRunner.run(context -> {
            // Then
            int filterChainCount = context.getBeansOfType(SecurityFilterChain.class).size();
            assertThat(filterChainCount).isEqualTo(1);
        });
    }

    @TestConfiguration
    static class TestSecurityBeans {
        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            return mock(JwtAuthenticationFilter.class);
        }
    }
}
