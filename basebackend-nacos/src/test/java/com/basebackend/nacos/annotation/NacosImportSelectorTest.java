package com.basebackend.nacos.annotation;

import com.basebackend.nacos.config.NacosConfigConfiguration;
import com.basebackend.nacos.config.NacosDiscoveryConfiguration;
import com.basebackend.nacos.config.NacosAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;

class NacosImportSelectorTest {
    private static final String CONFIG_ENABLED_PROPERTY = "nacos.config.enabled";
    private static final String DISCOVERY_ENABLED_PROPERTY = "nacos.discovery.enabled";

    private String previousConfigEnabled;
    private String previousDiscoveryEnabled;

    @BeforeEach
    void setUp() {
        this.previousConfigEnabled = System.getProperty(CONFIG_ENABLED_PROPERTY);
        this.previousDiscoveryEnabled = System.getProperty(DISCOVERY_ENABLED_PROPERTY);
    }

    @AfterEach
    void tearDown() {
        restoreProperty(CONFIG_ENABLED_PROPERTY, this.previousConfigEnabled);
        restoreProperty(DISCOVERY_ENABLED_PROPERTY, this.previousDiscoveryEnabled);
    }

    @Test
    void shouldApplyConfigToggleFromEnableNacosSupport() {
        NacosImportSelector selector = new NacosImportSelector();

        String[] imports = selector.selectImports(AnnotationMetadata.introspect(ConfigDisabledApp.class));

        assertThat(imports).containsExactly(
            NacosAutoConfiguration.class.getName(),
            NacosDiscoveryConfiguration.class.getName()
        );
    }

    @Test
    void shouldApplyDiscoveryToggleFromEnableNacosSupport() {
        NacosImportSelector selector = new NacosImportSelector();

        String[] imports = selector.selectImports(AnnotationMetadata.introspect(DiscoveryDisabledApp.class));

        assertThat(imports).containsExactly(
            NacosAutoConfiguration.class.getName(),
            NacosConfigConfiguration.class.getName()
        );
    }

    @Test
    void shouldImportAllWhenUsingDefaultToggleValues() {
        NacosImportSelector selector = new NacosImportSelector();

        String[] imports = selector.selectImports(AnnotationMetadata.introspect(DefaultEnabledApp.class));

        assertThat(imports).containsExactly(
            NacosAutoConfiguration.class.getName(),
            NacosConfigConfiguration.class.getName(),
            NacosDiscoveryConfiguration.class.getName()
        );
    }

    @Test
    void shouldKeepOnlyBaseAutoConfigurationWhenBothDisabled() {
        NacosImportSelector selector = new NacosImportSelector();

        String[] imports = selector.selectImports(AnnotationMetadata.introspect(AllDisabledApp.class));

        assertThat(imports).containsExactly(NacosAutoConfiguration.class.getName());
    }

    @Test
    void shouldNotMutateGlobalToggleProperties() {
        NacosImportSelector selector = new NacosImportSelector();
        System.setProperty(CONFIG_ENABLED_PROPERTY, "external-config-toggle");
        System.setProperty(DISCOVERY_ENABLED_PROPERTY, "external-discovery-toggle");

        selector.selectImports(AnnotationMetadata.introspect(ConfigDisabledApp.class));

        assertThat(System.getProperty(CONFIG_ENABLED_PROPERTY)).isEqualTo("external-config-toggle");
        assertThat(System.getProperty(DISCOVERY_ENABLED_PROPERTY)).isEqualTo("external-discovery-toggle");
    }

    @Test
    void shouldApplyToggleToCurrentEnvironmentOnly() {
        NacosImportSelector selector = new NacosImportSelector();
        MockEnvironment environment = new MockEnvironment()
            .withProperty(CONFIG_ENABLED_PROPERTY, "true")
            .withProperty(DISCOVERY_ENABLED_PROPERTY, "true");
        selector.setEnvironment(environment);

        selector.selectImports(AnnotationMetadata.introspect(AllDisabledApp.class));

        assertThat(environment.getProperty(CONFIG_ENABLED_PROPERTY)).isEqualTo("false");
        assertThat(environment.getProperty(DISCOVERY_ENABLED_PROPERTY)).isEqualTo("false");
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
            return;
        }
        System.setProperty(key, value);
    }

    @EnableNacosSupport(config = false, discovery = true)
    private static class ConfigDisabledApp {
    }

    @EnableNacosSupport(config = true, discovery = false)
    private static class DiscoveryDisabledApp {
    }

    @EnableNacosSupport(config = false, discovery = false)
    private static class AllDisabledApp {
    }

    @EnableNacosSupport
    private static class DefaultEnabledApp {
    }
}
