package com.basebackend.nacos.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NacosConfigValidatorTest {

    @Test
    void shouldRejectMalformedEncryptedPasswordInConfig() {
        NacosConfigProperties properties = new NacosConfigProperties();
        properties.getConfig().setUsername("nacos-user");
        properties.getConfig().setPassword("ENC(invalid");

        NacosConfigValidator validator = new NacosConfigValidator(properties);

        assertThatThrownBy(validator::validate)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("nacos.config.password")
            .hasMessageContaining("ENC(...)");
    }

    @Test
    void shouldAcceptWellFormedEncryptedPasswordInDiscovery() {
        NacosConfigProperties properties = new NacosConfigProperties();
        properties.getDiscovery().setUsername("nacos-user");
        properties.getDiscovery().setPassword("ENC(valid)");

        NacosConfigValidator validator = new NacosConfigValidator(properties);

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }
}
