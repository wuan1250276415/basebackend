package com.basebackend.common.idempotent.config;

import com.basebackend.common.idempotent.aspect.IdempotentAspect;
import com.basebackend.common.idempotent.store.IdempotentStore;
import com.basebackend.common.idempotent.token.IdempotentTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IdempotentAutoConfiguration 单元测试
 */
class IdempotentAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IdempotentAutoConfiguration.class));

    @Test
    @DisplayName("无 Redis 场景下应成功装配幂等切面")
    void shouldCreateAspectWithoutRedisTokenService() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(IdempotentStore.class);
            assertThat(context).hasSingleBean(IdempotentAspect.class);
            assertThat(context).doesNotHaveBean(IdempotentTokenService.class);
        });
    }
}

