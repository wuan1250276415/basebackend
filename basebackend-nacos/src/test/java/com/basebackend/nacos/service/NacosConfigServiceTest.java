package com.basebackend.nacos.service;

import com.alibaba.nacos.api.config.ConfigService;
import com.basebackend.nacos.isolation.ConfigIsolationManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NacosConfigServiceTest {

    @Test
    void shouldCalculateMd5() {
        ConfigService configService = mock(ConfigService.class);
        NacosConfigService nacosConfigService = new NacosConfigService(configService, new ConfigIsolationManager());

        assertThat(nacosConfigService.calculateMd5("abc")).isEqualTo("900150983cd24fb0d6963f7d28e17f72");
        assertThat(nacosConfigService.calculateMd5(null)).isNull();
    }
}
