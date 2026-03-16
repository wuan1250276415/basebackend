package com.basebackend.nacos.annotation;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.naming.NamingService;
import com.basebackend.nacos.config.NacosAutoConfiguration;
import com.basebackend.nacos.config.NacosConfigConfiguration;
import com.basebackend.nacos.config.NacosConfigValidator;
import com.basebackend.nacos.config.NacosDiscoveryConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EnableNacosSupportToggleIntegrationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            NacosAutoConfiguration.class,
            NacosConfigConfiguration.class,
            NacosDiscoveryConfiguration.class
        ))
        .withBean(NacosConfigValidator.class, () -> mock(NacosConfigValidator.class))
        .withBean(ConfigService.class, () -> mock(ConfigService.class))
        .withBean(NamingService.class, () -> mock(NamingService.class));

    @Test
    void shouldDisableBothConfigurationsInAutoConfigurationChain() {
        this.contextRunner
            .withUserConfiguration(AllDisabledApp.class)
            .run(context -> {
                assertThat(context.getBeansOfType(NacosConfigConfiguration.class)).isEmpty();
                assertThat(context.getBeansOfType(NacosDiscoveryConfiguration.class)).isEmpty();
                assertThat(context.getEnvironment().getProperty("nacos.config.enabled")).isEqualTo("false");
                assertThat(context.getEnvironment().getProperty("nacos.discovery.enabled")).isEqualTo("false");
            });
    }

    @Test
    void shouldOnlyEnableDiscoveryWhenConfigIsDisabled() {
        this.contextRunner
            .withUserConfiguration(ConfigDisabledApp.class)
            .run(context -> {
                assertThat(context.getBeansOfType(NacosConfigConfiguration.class)).isEmpty();
                assertThat(context.getBeansOfType(NacosDiscoveryConfiguration.class)).hasSize(1);
                assertThat(context.getEnvironment().getProperty("nacos.config.enabled")).isEqualTo("false");
                assertThat(context.getEnvironment().getProperty("nacos.discovery.enabled")).isEqualTo("true");
            });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableNacosSupport(config = false, discovery = false)
    static class AllDisabledApp {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableNacosSupport(config = false, discovery = true)
    static class ConfigDisabledApp {
    }
}
