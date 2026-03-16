package com.basebackend.nacos.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NacosConfigPropertiesTest {

    @Test
    void shouldExposeExpectedDefaults() {
        NacosConfigProperties properties = new NacosConfigProperties();

        assertThat(properties.getEnvironment()).isEqualTo("dev");
        assertThat(properties.getTenantId()).isEqualTo("public");
        assertThat(properties.getConfig().getServerAddr()).isEqualTo("127.0.0.1:8848");
        assertThat(properties.getDiscovery().getServerAddr()).isEqualTo("127.0.0.1:8848");
        assertThat(properties.getConfig().getUsername()).isEmpty();
        assertThat(properties.getConfig().getPassword()).isEmpty();
        assertThat(properties.getDiscovery().getUsername()).isEmpty();
        assertThat(properties.getDiscovery().getPassword()).isEmpty();
        assertThat(properties.getConfig().getSharedConfigs()).isEmpty();
        assertThat(properties.getConfig().getExtensionConfigs()).isEmpty();
    }

    @Test
    void shouldMaskSensitiveValuesInToString() {
        NacosConfigProperties properties = new NacosConfigProperties();

        properties.getConfig().setUsername("custom-user");
        properties.getConfig().setPassword("custom-password");
        properties.getDiscovery().setUsername("discovery-user");
        properties.getDiscovery().setPassword("discovery-password");

        assertThat(properties.getConfig().toString())
            .contains("username=******")
            .contains("password=******")
            .doesNotContain("custom-user")
            .doesNotContain("custom-password");

        assertThat(properties.getDiscovery().toString())
            .contains("username=******")
            .contains("password=******")
            .doesNotContain("discovery-user")
            .doesNotContain("discovery-password");
    }
}
