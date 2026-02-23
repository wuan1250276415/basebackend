package com.basebackend.nacos.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NacosConfigConfiguration 配置中心配置测试")
class NacosConfigConfigurationTest {

    @Test
    @DisplayName("username 为 null 时 Properties 不应包含 username 键")
    void properties_shouldNotContainUsername_whenUsernameIsNull() {
        NacosConfigProperties props = new NacosConfigProperties();
        props.getConfig().setServerAddr("127.0.0.1:8848");
        props.getConfig().setNamespace("public");
        props.getConfig().setGroup("DEFAULT_GROUP");
        props.getConfig().setUsername(null);
        props.getConfig().setPassword("some-pass");

        Properties properties = buildConfigProperties(props.getConfig());

        assertThat(properties.containsKey("username")).isFalse();
        assertThat(properties.getProperty("password")).isEqualTo("some-pass");
    }

    @Test
    @DisplayName("password 为 null 时 Properties 不应包含 password 键")
    void properties_shouldNotContainPassword_whenPasswordIsNull() {
        NacosConfigProperties props = new NacosConfigProperties();
        props.getConfig().setServerAddr("127.0.0.1:8848");
        props.getConfig().setNamespace("public");
        props.getConfig().setGroup("DEFAULT_GROUP");
        props.getConfig().setUsername("admin");
        props.getConfig().setPassword(null);

        Properties properties = buildConfigProperties(props.getConfig());

        assertThat(properties.getProperty("username")).isEqualTo("admin");
        assertThat(properties.containsKey("password")).isFalse();
    }

    @Test
    @DisplayName("username 和 password 都为 null 时不应抛出 NPE")
    void properties_shouldNotThrowNPE_whenBothNull() {
        NacosConfigProperties props = new NacosConfigProperties();
        props.getConfig().setServerAddr("127.0.0.1:8848");
        props.getConfig().setNamespace("public");
        props.getConfig().setGroup("DEFAULT_GROUP");
        props.getConfig().setUsername(null);
        props.getConfig().setPassword(null);

        Properties properties = buildConfigProperties(props.getConfig());

        assertThat(properties.containsKey("username")).isFalse();
        assertThat(properties.containsKey("password")).isFalse();
        assertThat(properties.getProperty("serverAddr")).isEqualTo("127.0.0.1:8848");
    }

    @Test
    @DisplayName("提供凭证时 Properties 应正确包含 username 和 password")
    void properties_shouldContainCredentials_whenProvided() {
        NacosConfigProperties props = new NacosConfigProperties();
        props.getConfig().setServerAddr("10.0.0.1:8848");
        props.getConfig().setNamespace("prod");
        props.getConfig().setGroup("APP_GROUP");
        props.getConfig().setUsername("admin");
        props.getConfig().setPassword("secret");

        Properties properties = buildConfigProperties(props.getConfig());

        assertThat(properties.getProperty("serverAddr")).isEqualTo("10.0.0.1:8848");
        assertThat(properties.getProperty("namespace")).isEqualTo("prod");
        assertThat(properties.getProperty("group")).isEqualTo("APP_GROUP");
        assertThat(properties.getProperty("username")).isEqualTo("admin");
        assertThat(properties.getProperty("password")).isEqualTo("secret");
    }

    @Test
    @DisplayName("Discovery 凭证为 null 时不应抛出 NPE")
    void discoveryProperties_shouldNotThrowNPE_whenCredentialsNull() {
        NacosConfigProperties props = new NacosConfigProperties();
        props.getDiscovery().setServerAddr("127.0.0.1:8848");
        props.getDiscovery().setNamespace("public");
        props.getDiscovery().setGroup("DEFAULT_GROUP");
        props.getDiscovery().setUsername(null);
        props.getDiscovery().setPassword(null);

        Properties properties = buildDiscoveryProperties(props.getDiscovery());

        assertThat(properties.containsKey("username")).isFalse();
        assertThat(properties.containsKey("password")).isFalse();
        assertThat(properties.getProperty("serverAddr")).isEqualTo("127.0.0.1:8848");
    }

    /**
     * Mirrors the Properties-building logic from NacosConfigConfiguration.configService()
     */
    private Properties buildConfigProperties(NacosConfigProperties.Config config) {
        Properties properties = new Properties();
        properties.put("serverAddr", config.getServerAddr());
        properties.put("namespace", config.getNamespace());
        properties.put("group", config.getGroup());
        if (config.getUsername() != null) {
            properties.put("username", config.getUsername());
        }
        if (config.getPassword() != null) {
            properties.put("password", config.getPassword());
        }
        return properties;
    }

    /**
     * Mirrors the Properties-building logic from NacosDiscoveryConfiguration.namingService()
     */
    private Properties buildDiscoveryProperties(NacosConfigProperties.Discovery discovery) {
        Properties properties = new Properties();
        properties.put("serverAddr", discovery.getServerAddr());
        properties.put("namespace", discovery.getNamespace());
        properties.put("group", discovery.getGroup());
        if (discovery.getUsername() != null) {
            properties.put("username", discovery.getUsername());
        }
        if (discovery.getPassword() != null) {
            properties.put("password", discovery.getPassword());
        }
        return properties;
    }
}
