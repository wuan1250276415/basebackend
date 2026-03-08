package com.basebackend.nacos.config;

import com.basebackend.nacos.security.CredentialEncryptionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NacosConfigConfigurationTest {
    @Test
    void shouldConstructConfigurations() {
        NacosConfigProperties properties = new NacosConfigProperties();
        CredentialEncryptionService encryptionService = mock(CredentialEncryptionService.class);
        when(encryptionService.decryptIfNeeded(any())).thenAnswer(invocation -> invocation.getArgument(0));
        MockEnvironment environment = new MockEnvironment()
            .withProperty("nacos.encryption.key", "test-key-seed")
            .withProperty("nacos.encryption.fail-on-default-key", "false");

        NacosConfigConfiguration configConfiguration = new NacosConfigConfiguration(properties, encryptionService);
        NacosDiscoveryConfiguration discoveryConfiguration = new NacosDiscoveryConfiguration(properties, encryptionService);
        NacosAutoConfiguration autoConfiguration = new NacosAutoConfiguration(properties, environment);

        configConfiguration.init();
        discoveryConfiguration.init();

        assertThat(configConfiguration).isNotNull();
        assertThat(discoveryConfiguration).isNotNull();
        assertThat(autoConfiguration.nacosConfigValidator()).isNotNull();
        assertThat(autoConfiguration.grayReleaseHistoryRepository()).isNotNull();
        assertThat(autoConfiguration.credentialEncryptionService()).isNotNull();
    }

    @Test
    void shouldSkipBlankCredentialsWhenBuildingClientProperties() {
        NacosConfigProperties properties = new NacosConfigProperties();
        properties.getConfig().setUsername("");
        properties.getConfig().setPassword("");
        properties.getDiscovery().setUsername("");
        properties.getDiscovery().setPassword("");

        CredentialEncryptionService encryptionService = mock(CredentialEncryptionService.class);
        when(encryptionService.decryptIfNeeded(any())).thenAnswer(invocation -> invocation.getArgument(0));

        NacosConfigConfiguration configConfiguration = new NacosConfigConfiguration(properties, encryptionService);
        NacosDiscoveryConfiguration discoveryConfiguration = new NacosDiscoveryConfiguration(properties, encryptionService);

        Properties configClientProperties = configConfiguration.buildClientProperties(properties.getConfig());
        Properties discoveryClientProperties = discoveryConfiguration.buildClientProperties(properties.getDiscovery());

        assertThat(configClientProperties).doesNotContainKeys("username", "password");
        assertThat(discoveryClientProperties).doesNotContainKeys("username", "password");
    }

    @Test
    void shouldDecryptCredentialsWhenBuildingClientProperties() {
        NacosConfigProperties properties = new NacosConfigProperties();
        properties.getConfig().setUsername("ENC(config-user)");
        properties.getConfig().setPassword("ENC(config-password)");
        properties.getDiscovery().setUsername("ENC(discovery-user)");
        properties.getDiscovery().setPassword("ENC(discovery-password)");

        CredentialEncryptionService encryptionService = mock(CredentialEncryptionService.class);
        when(encryptionService.decryptIfNeeded("ENC(config-user)")).thenReturn("config-user");
        when(encryptionService.decryptIfNeeded("ENC(config-password)")).thenReturn("config-password");
        when(encryptionService.decryptIfNeeded("ENC(discovery-user)")).thenReturn("discovery-user");
        when(encryptionService.decryptIfNeeded("ENC(discovery-password)")).thenReturn("discovery-password");

        NacosConfigConfiguration configConfiguration = new NacosConfigConfiguration(properties, encryptionService);
        NacosDiscoveryConfiguration discoveryConfiguration = new NacosDiscoveryConfiguration(properties, encryptionService);

        Properties configClientProperties = configConfiguration.buildClientProperties(properties.getConfig());
        Properties discoveryClientProperties = discoveryConfiguration.buildClientProperties(properties.getDiscovery());

        assertThat(configClientProperties.getProperty("username")).isEqualTo("config-user");
        assertThat(configClientProperties.getProperty("password")).isEqualTo("config-password");
        assertThat(discoveryClientProperties.getProperty("username")).isEqualTo("discovery-user");
        assertThat(discoveryClientProperties.getProperty("password")).isEqualTo("discovery-password");

        verify(encryptionService).decryptIfNeeded("ENC(config-user)");
        verify(encryptionService).decryptIfNeeded("ENC(config-password)");
        verify(encryptionService).decryptIfNeeded("ENC(discovery-user)");
        verify(encryptionService).decryptIfNeeded("ENC(discovery-password)");
    }

}
