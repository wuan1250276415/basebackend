package com.basebackend.nacos.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nacos配置属性测试
 * 测试配置解析、默认值、校验规则
 *
 * @author BaseBackend
 */
@DisplayName("NacosConfigProperties Nacos配置属性测试")
class NacosConfigPropertiesTest {

    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    @Test
    @DisplayName("默认配置应该正确初始化")
    void shouldHaveCorrectDefaultValues() {
        // Given
        NacosConfigProperties properties = new NacosConfigProperties();

        // Then
        assertThat(properties.getEnvironment()).isEqualTo("dev");
        assertThat(properties.getTenantId()).isEqualTo("public");
        assertThat(properties.getConfig().getServerAddr()).isEqualTo("192.168.66.126:8848");
        assertThat(properties.getConfig().getNamespace()).isEqualTo("public");
        assertThat(properties.getConfig().getGroup()).isEqualTo("DEFAULT_GROUP");
        assertThat(properties.getConfig().getUsername()).isEqualTo("nacos");
        assertThat(properties.getConfig().getPassword()).isEqualTo("nacos");
        List<NacosConfigProperties.Config.SharedConfig> sharedConfigs = new ArrayList<>();
        NacosConfigProperties.Config.SharedConfig s1 = new NacosConfigProperties.Config.SharedConfig();
        s1.setDataId("common-config.yml");
        s1.setRefresh(Boolean.TRUE);
        NacosConfigProperties.Config.SharedConfig s2 = new NacosConfigProperties.Config.SharedConfig();
        s2.setDataId("mysql-config.yml");
        s2.setRefresh(Boolean.TRUE);
        NacosConfigProperties.Config.SharedConfig s3 = new NacosConfigProperties.Config.SharedConfig();
        s3.setDataId("observability-config.yml");
        s3.setRefresh(Boolean.TRUE);
        NacosConfigProperties.Config.SharedConfig s4 = new NacosConfigProperties.Config.SharedConfig();
        s4.setDataId("redis-config.yml");
        s4.setRefresh(Boolean.TRUE);
        NacosConfigProperties.Config.SharedConfig s5 = new NacosConfigProperties.Config.SharedConfig();
        s5.setDataId("rocketmq-config.yml");
        s5.setRefresh(Boolean.TRUE);
        NacosConfigProperties.Config.SharedConfig s6 = new NacosConfigProperties.Config.SharedConfig();
        s6.setDataId("backup-config.yml");
        s6.setRefresh(Boolean.TRUE);
        NacosConfigProperties.Config.SharedConfig s7 = new NacosConfigProperties.Config.SharedConfig();
        s6.setDataId("security-config.yml");
        s6.setRefresh(Boolean.TRUE);
        NacosConfigProperties.Config.SharedConfig s8 = new NacosConfigProperties.Config.SharedConfig();
        s6.setDataId("application-web-config.yml");
        s6.setRefresh(Boolean.TRUE);
        sharedConfigs.add(s1);
        sharedConfigs.add(s2);
        sharedConfigs.add(s3);
        sharedConfigs.add(s4);
        sharedConfigs.add(s5);
        sharedConfigs.add(s6);
        sharedConfigs.add(s7);
        sharedConfigs.add(s8);
        assertThat(properties.getConfig().getSharedConfigs()).isEqualTo(sharedConfigs);
        assertThat(properties.getConfig().isEnabled()).isTrue();
        assertThat(properties.getDiscovery().isEnabled()).isTrue();
        assertThat(properties.getDiscovery().getServerAddr()).isEqualTo("192.168.66.126:8848");
        assertThat(properties.getDiscovery().getNamespace()).isEqualTo("public");
        assertThat(properties.getDiscovery().getCluster()).isEqualTo("DEFAULT");
        assertThat(properties.getDiscovery().getUsername()).isEqualTo("nacos");
        assertThat(properties.getDiscovery().getPassword()).isEqualTo("nacos");
        assertThat(properties.getDiscovery().getWeight()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("环境配置不能为空")
    void shouldRejectNullEnvironment() {
        // Given
        NacosConfigProperties properties = new NacosConfigProperties();
        properties.setEnvironment(null);

        // When
        Set<ConstraintViolation<NacosConfigProperties>> violations = validator.validate(properties);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("环境配置不能为空");
    }

    @Test
    @DisplayName("空环境配置应该被拒绝")
    void shouldRejectEmptyEnvironment() {
        // Given
        NacosConfigProperties properties = new NacosConfigProperties();
        properties.setEnvironment("");

        // When
        Set<ConstraintViolation<NacosConfigProperties>> violations = validator.validate(properties);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("环境配置不能为空");
    }

    @Test
    @DisplayName("配置中心地址不能为空")
    void shouldRejectNullConfigServerAddr() {
        // Given
        NacosConfigProperties properties = new NacosConfigProperties();
        properties.getConfig().setServerAddr(null);

        // When
        Set<ConstraintViolation<NacosConfigProperties>> violations = validator.validate(properties);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("nacos.config.server-addr 不能为空");
    }

    @Test
    @DisplayName("服务发现地址不能为空")
    void shouldRejectNullDiscoveryServerAddr() {
        // Given
        NacosConfigProperties properties = new NacosConfigProperties();
        properties.getDiscovery().setServerAddr(null);

        // When
        Set<ConstraintViolation<NacosConfigProperties>> violations = validator.validate(properties);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("nacos.discovery.server-addr 不能为空");
    }

    @Test
    @DisplayName("共享配置数据ID不能为空")
    void shouldRejectNullSharedConfigDataId() {
        // Given
        NacosConfigProperties properties = new NacosConfigProperties();
        NacosConfigProperties.Config.SharedConfig s1 = new NacosConfigProperties.Config.SharedConfig();
        s1.setDataId("common-config.yml");
        s1.setRefresh(Boolean.TRUE);
        properties.getConfig().getSharedConfigs().add(s1);

        // When
        Set<ConstraintViolation<NacosConfigProperties>> violations = validator.validate(properties);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("dataId");
    }

    @Test
    @DisplayName("扩展配置数据ID不能为空")
    void shouldRejectNullExtensionConfigDataId() {
        // Given
        NacosConfigProperties properties = new NacosConfigProperties();
        NacosConfigProperties.Config.ExtensionConfig extensionConfig = new NacosConfigProperties.Config.ExtensionConfig();
        extensionConfig.setDataId(null);
        properties.getConfig().getExtensionConfigs().add(extensionConfig);

        // When
        Set<ConstraintViolation<NacosConfigProperties>> violations = validator.validate(properties);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("dataId");
    }

    @Test
    @DisplayName("正确配置应该通过校验")
    void shouldPassValidationWithCorrectConfig() {
        // Given
        NacosConfigProperties properties = new NacosConfigProperties();
        properties.setEnvironment("dev");
        properties.getConfig().setServerAddr("127.0.0.1:8848");
        properties.getDiscovery().setServerAddr("127.0.0.1:8848");

        // When
        Set<ConstraintViolation<NacosConfigProperties>> violations = validator.validate(properties);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("应该支持多租户配置")
    void shouldSupportMultiTenantConfig() {
        // Given
        NacosConfigProperties properties = new NacosConfigProperties();
        properties.setTenantId("tenant-001");

        // Then
        assertThat(properties.getTenantId()).isEqualTo("tenant-001");
    }

    @Test
    @DisplayName("应该支持应用ID配置")
    void shouldSupportAppIdConfig() {
        // Given
        NacosConfigProperties properties = new NacosConfigProperties();
        properties.setAppId(123L);

        // Then
        assertThat(properties.getAppId()).isEqualTo(123L);
    }

    @Test
    @DisplayName("应该支持配置中心禁用")
    void shouldSupportConfigCenterDisabled() {
        // Given
        NacosConfigProperties properties = new NacosConfigProperties();
        properties.getConfig().setEnabled(false);

        // Then
        assertThat(properties.getConfig().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("应该支持服务发现禁用")
    void shouldSupportServiceDiscoveryDisabled() {
        // Given
        NacosConfigProperties properties = new NacosConfigProperties();
        properties.getDiscovery().setEnabled(false);

        // Then
        assertThat(properties.getDiscovery().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("应该支持共享配置列表")
    void shouldSupportSharedConfigList() {
        // Given
        NacosConfigProperties properties = new NacosConfigProperties();
        NacosConfigProperties.Config.SharedConfig sharedConfig = new NacosConfigProperties.Config.SharedConfig();
        sharedConfig.setDataId("shared-config.yml");
        sharedConfig.setGroup("SHARED_GROUP");
        sharedConfig.setRefresh(true);
        properties.getConfig().getSharedConfigs().add(sharedConfig);

        // Then
        assertThat(properties.getConfig().getSharedConfigs()).hasSize(1);
        assertThat(properties.getConfig().getSharedConfigs().get(0).getDataId()).isEqualTo("shared-config.yml");
        assertThat(properties.getConfig().getSharedConfigs().get(0).getGroup()).isEqualTo("SHARED_GROUP");
        assertThat(properties.getConfig().getSharedConfigs().get(0).isRefresh()).isTrue();
    }

    @Test
    @DisplayName("应该支持扩展配置列表")
    void shouldSupportExtensionConfigList() {
        // Given
        NacosConfigProperties properties = new NacosConfigProperties();
        NacosConfigProperties.Config.ExtensionConfig extensionConfig = new NacosConfigProperties.Config.ExtensionConfig();
        extensionConfig.setDataId("extension-config.yml");
        extensionConfig.setGroup("EXT_GROUP");
        extensionConfig.setRefresh(false);
        properties.getConfig().getExtensionConfigs().add(extensionConfig);

        // Then
        assertThat(properties.getConfig().getExtensionConfigs()).hasSize(1);
        assertThat(properties.getConfig().getExtensionConfigs().get(0).getDataId()).isEqualTo("extension-config.yml");
        assertThat(properties.getConfig().getExtensionConfigs().get(0).getGroup()).isEqualTo("EXT_GROUP");
        assertThat(properties.getConfig().getExtensionConfigs().get(0).isRefresh()).isFalse();
    }

    @Test
    @DisplayName("应该支持实例元数据")
    void shouldSupportInstanceMetadata() {
        // Given
        NacosConfigProperties properties = new NacosConfigProperties();
        properties.getDiscovery().getMetadata().put("version", "1.0.0");
        properties.getDiscovery().getMetadata().put("region", "us-east-1");

        // Then
        assertThat(properties.getDiscovery().getMetadata()).hasSize(2);
        assertThat(properties.getDiscovery().getMetadata().get("version")).isEqualTo("1.0.0");
        assertThat(properties.getDiscovery().getMetadata().get("region")).isEqualTo("us-east-1");
    }

    @Test
    @DisplayName("应该支持自定义环境")
    void shouldSupportCustomEnvironment() {
        // Given
        NacosConfigProperties properties = new NacosConfigProperties();
        properties.setEnvironment("prod");

        // Then
        assertThat(properties.getEnvironment()).isEqualTo("prod");
    }
}
