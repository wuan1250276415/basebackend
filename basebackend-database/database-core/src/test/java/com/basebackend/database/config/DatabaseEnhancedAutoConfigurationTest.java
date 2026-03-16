package com.basebackend.database.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DatabaseEnhancedAutoConfiguration 自动配置测试")
class DatabaseEnhancedAutoConfigurationTest {

    @Test
    @DisplayName("条件前缀应与 DatabaseEnhancedProperties 保持一致")
    void shouldUseDatabaseEnhancedPrefixInConditionalOnProperty() {
        ConditionalOnProperty conditionalOnProperty =
            DatabaseEnhancedAutoConfiguration.class.getAnnotation(ConditionalOnProperty.class);

        assertThat(conditionalOnProperty).isNotNull();
        assertThat(conditionalOnProperty.prefix()).isEqualTo("database.enhanced");
        assertThat(conditionalOnProperty.name()).contains("enabled");
    }
}
