package com.basebackend.gateway.cors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.*;

@DisplayName("DynamicCorsProperties 测试")
class DynamicCorsPropertiesTest {

    private DynamicCorsProperties props;

    @BeforeEach
    void setUp() {
        props = new DynamicCorsProperties();
    }

    @Test
    @DisplayName("默认值正确")
    void defaults() {
        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getAllowedOrigins()).isEmpty();
        assertThat(props.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE", "OPTIONS");
        assertThat(props.getAllowedHeaders()).contains("*");
        assertThat(props.isAllowCredentials()).isTrue();
        assertThat(props.getMaxAge()).isEqualTo(3600);
    }

    @Test
    @DisplayName("动态添加允许源")
    void addAllowedOrigin() {
        props.addAllowedOrigin("http://localhost:3000");
        assertThat(props.getAllowedOrigins()).contains("http://localhost:3000");
    }

    @Test
    @DisplayName("重复添加不会重复")
    void addDuplicateOrigin() {
        props.addAllowedOrigin("http://localhost:3000");
        props.addAllowedOrigin("http://localhost:3000");
        assertThat(props.getAllowedOrigins()).hasSize(1);
    }

    @Test
    @DisplayName("动态移除允许源")
    void removeAllowedOrigin() {
        props.addAllowedOrigin("http://localhost:3000");
        props.removeAllowedOrigin("http://localhost:3000");
        assertThat(props.getAllowedOrigins()).isEmpty();
    }

    @Test
    @DisplayName("转换为 CorsConfiguration — 有配置源")
    void toCorsConfigurationWithOrigins() {
        props.addAllowedOrigin("http://localhost:3000");
        props.addAllowedOrigin("https://admin.example.com");

        CorsConfiguration config = props.toCorsConfiguration();
        assertThat(config.getAllowedOrigins()).containsExactly(
                "http://localhost:3000", "https://admin.example.com"
        );
        assertThat(config.getAllowedMethods()).contains("GET", "POST");
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getMaxAge()).isEqualTo(3600);
    }

    @Test
    @DisplayName("转换为 CorsConfiguration — 无配置源默认 *")
    void toCorsConfigurationDefaultWildcard() {
        CorsConfiguration config = props.toCorsConfiguration();
        assertThat(config.getAllowedOrigins()).containsExactly("*");
    }
}
