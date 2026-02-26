package com.basebackend.common.starter.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CommonProperties + UserContextProperties 单元测试
 */
class CommonPropertiesTest {

    // ========== CommonProperties ==========

    @Nested
    @DisplayName("CommonProperties 默认值")
    class CommonDefaults {

        @Test
        @DisplayName("模块默认启用")
        void shouldBeEnabledByDefault() {
            var props = new CommonProperties();
            assertThat(props.getEnabled()).isTrue();
        }

        @Test
        @DisplayName("异常处理默认配置")
        void shouldHaveExceptionDefaults() {
            var ex = new CommonProperties.ExceptionProperties();
            assertThat(ex.getEnabled()).isTrue();
            assertThat(ex.getIncludeStackTrace()).isFalse();
            assertThat(ex.getLogEnabled()).isTrue();
            assertThat(ex.getLogRequestInfo()).isTrue();
        }

        @Test
        @DisplayName("Jackson 默认配置")
        void shouldHaveJacksonDefaults() {
            var jackson = new CommonProperties.JacksonProperties();
            assertThat(jackson.getEnabled()).isTrue();
            assertThat(jackson.getDateFormat()).isEqualTo("yyyy-MM-dd HH:mm:ss");
            assertThat(jackson.getTimeZone()).isEqualTo("GMT+8");
            assertThat(jackson.getIncludeNulls()).isFalse();
            assertThat(jackson.getSnakeCaseEnabled()).isFalse();
            assertThat(jackson.getFailOnUnknownProperties()).isFalse();
        }

        @Test
        @DisplayName("Context 默认配置")
        void shouldHaveContextDefaults() {
            var ctx = new CommonProperties.ContextProperties();
            assertThat(ctx.getAutoCleanup()).isTrue();
            assertThat(ctx.getFilterOrder()).isEqualTo(Integer.MIN_VALUE + 100);
        }

        @Test
        @DisplayName("生产环境安全: stackTrace 默认关闭")
        void shouldHideStackTraceByDefault() {
            var props = new CommonProperties();
            assertThat(props.getException().getIncludeStackTrace()).isFalse();
        }
    }

    // ========== UserContextProperties ==========

    @Nested
    @DisplayName("UserContextProperties 默认值")
    class UserContextDefaults {

        @Test
        @DisplayName("默认启用")
        void shouldBeEnabledByDefault() {
            var props = new com.basebackend.common.starter.config.UserContextProperties();
            assertThat(props.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("默认拦截所有路径")
        void shouldInterceptAllPaths() {
            var props = new com.basebackend.common.starter.config.UserContextProperties();
            assertThat(props.getPathPatterns()).containsExactly("/**");
        }

        @Test
        @DisplayName("默认排除登录/注册/Swagger/Actuator")
        void shouldExcludeCommonPaths() {
            var props = new com.basebackend.common.starter.config.UserContextProperties();
            assertThat(props.getExcludePatterns())
                    .contains("/**/auth/login", "/**/auth/register",
                            "/swagger-ui/**", "/v3/api-docs/**", "/actuator/**");
        }

        @Test
        @DisplayName("排除路径数量合理")
        void shouldHaveReasonableExcludeCount() {
            var props = new com.basebackend.common.starter.config.UserContextProperties();
            assertThat(props.getExcludePatterns()).hasSizeGreaterThanOrEqualTo(5);
        }

        @Test
        @DisplayName("默认排序为 10")
        void shouldHaveDefaultOrder() {
            var props = new com.basebackend.common.starter.config.UserContextProperties();
            assertThat(props.getOrder()).isEqualTo(10);
        }
    }
}
