package com.basebackend.nacos.service;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.basebackend.nacos.model.ConfigInfo;
import com.basebackend.nacos.model.GrayReleaseConfig;
import com.basebackend.nacos.model.GrayReleaseHistory;
import com.basebackend.nacos.repository.GrayReleaseHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrayReleaseServiceTest {

    @Test
    void shouldFailWhenGrayConfigIsNull() {
        NacosConfigService nacosConfigService = mock(NacosConfigService.class);
        NamingService namingService = mock(NamingService.class);
        GrayReleaseHistoryRepository historyRepository = mock(GrayReleaseHistoryRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        GrayReleaseService service = new GrayReleaseService(
            nacosConfigService,
            namingService,
            new ObjectMapper(),
            historyRepository,
            eventPublisher
        );

        ConfigInfo configInfo = ConfigInfo.builder().dataId("order-config.yml").build();

        GrayReleaseService.GrayReleaseResult result = service.startGrayRelease(configInfo, null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("灰度发布启动失败");
    }

    @Test
    void shouldUseConfiguredGroupWhenStartingGrayRelease() throws Exception {
        NacosConfigService nacosConfigService = mock(NacosConfigService.class);
        NamingService namingService = mock(NamingService.class);
        GrayReleaseHistoryRepository historyRepository = mock(GrayReleaseHistoryRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        GrayReleaseService service = new GrayReleaseService(
            nacosConfigService,
            namingService,
            new ObjectMapper(),
            historyRepository,
            eventPublisher
        );

        Instance instance = new Instance();
        instance.setIp("10.0.0.1");
        instance.setPort(8080);

        when(namingService.getAllInstances("order", "GRAY_GROUP")).thenReturn(List.of(instance));
        when(nacosConfigService.publishConfig(any(ConfigInfo.class))).thenReturn(true);
        when(historyRepository.save(any(GrayReleaseHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfigInfo configInfo = ConfigInfo.builder()
            .dataId("order-config.yml")
            .group("GRAY_GROUP")
            .content("feature.flag=true")
            .build();

        GrayReleaseConfig grayConfig = GrayReleaseConfig.builder()
            .dataId("order-config.yml")
            .strategyType("percentage")
            .percentage(100)
            .build();

        GrayReleaseService.GrayReleaseResult result = service.startGrayRelease(configInfo, grayConfig);

        assertThat(result.isSuccess()).isTrue();
        verify(namingService, atLeastOnce()).getAllInstances("order", "GRAY_GROUP");
        verify(namingService).registerInstance(eq("order"), eq("GRAY_GROUP"), argThat(updated ->
            "true".equals(updated.getMetadata().get("gray-release")) &&
                "order-config.yml".equals(updated.getMetadata().get("gray-data-id"))
        ));
    }
}
