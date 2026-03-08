package com.basebackend.cache.admin;

import com.basebackend.cache.service.CacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * CacheAdminAutoConfiguration 条件装配测试
 */
class CacheAdminAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(CacheService.class, () -> mock(CacheService.class))
            .withConfiguration(AutoConfigurations.of(CacheAdminAutoConfiguration.class));

    @Test
    @DisplayName("enabled=true 且 actuator 存在时注册 endpoint")
    void shouldRegisterEndpointWhenEnabledAndActuatorPresent() {
        contextRunner
                .withPropertyValues("basebackend.cache.admin.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(CacheAdminEndpoint.class);
                });
    }

    @Test
    @DisplayName("enabled=false 时不注册 endpoint")
    void shouldNotRegisterEndpointWhenDisabled() {
        contextRunner
                .withPropertyValues("basebackend.cache.admin.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(CacheAdminEndpoint.class);
                });
    }

    @Test
    @DisplayName("enabled=true 但 actuator 缺失时不注册 endpoint")
    void shouldNotRegisterEndpointWhenActuatorMissing() {
        contextRunner
                .withPropertyValues("basebackend.cache.admin.enabled=true")
                .withClassLoader(new FilteredClassLoader("org.springframework.boot.actuate.endpoint.annotation"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(CacheAdminEndpoint.class);
                });
    }
}
