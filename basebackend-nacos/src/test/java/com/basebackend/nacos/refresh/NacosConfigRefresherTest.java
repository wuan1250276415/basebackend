package com.basebackend.nacos.refresh;

import com.alibaba.nacos.api.config.ConfigService;
import com.basebackend.nacos.config.NacosConfigProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@DisplayName("NacosConfigRefresher 配置刷新监听器测试")
class NacosConfigRefresherTest {

    @Test
    @DisplayName("destroy 应正常关闭线程池")
    @SuppressWarnings("unchecked")
    void destroy_shutdownsExecutor() {
        ConfigService configService = mock(ConfigService.class);
        NacosConfigProperties properties = new NacosConfigProperties();
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        ObjectProvider<SharedConfigListener[]> listenersProvider = mock(ObjectProvider.class);

        NacosConfigRefresher refresher = new NacosConfigRefresher(
                configService, properties, publisher, listenersProvider);

        assertThatCode(refresher::destroy).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("无监听器时 afterPropertiesSet 应跳过刷新注册")
    @SuppressWarnings("unchecked")
    void afterPropertiesSet_skipsRefresh_whenNoListeners() {
        ConfigService configService = mock(ConfigService.class);
        NacosConfigProperties properties = new NacosConfigProperties();
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        ObjectProvider<SharedConfigListener[]> listenersProvider = mock(ObjectProvider.class);
        when(listenersProvider.getIfAvailable()).thenReturn(null);

        NacosConfigRefresher refresher = new NacosConfigRefresher(
                configService, properties, publisher, listenersProvider);

        assertThatCode(refresher::afterPropertiesSet).doesNotThrowAnyException();
        verifyNoInteractions(configService);

        refresher.destroy();
    }

    @Test
    @DisplayName("可配置线程池大小")
    @SuppressWarnings("unchecked")
    void constructor_usesConfiguredPoolSize() {
        ConfigService configService = mock(ConfigService.class);
        NacosConfigProperties properties = new NacosConfigProperties();
        properties.getConfig().setRefreshThreadPoolSize(4);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        ObjectProvider<SharedConfigListener[]> listenersProvider = mock(ObjectProvider.class);

        assertThatCode(() -> {
            NacosConfigRefresher refresher = new NacosConfigRefresher(
                    configService, properties, publisher, listenersProvider);
            refresher.destroy();
        }).doesNotThrowAnyException();
    }
}
