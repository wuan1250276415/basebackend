package com.basebackend.nacos.refresh;

import com.alibaba.nacos.api.config.ConfigService;
import com.basebackend.nacos.config.NacosConfigProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NacosConfigRefresherTest {

    @Test
    void shouldSkipRegistrationWhenRefreshDisabled() {
        ConfigService configService = mock(ConfigService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SharedConfigListener[]> listenersProvider = mock(ObjectProvider.class);

        when(listenersProvider.getIfAvailable()).thenReturn(new SharedConfigListener[0]);

        NacosConfigProperties properties = new NacosConfigProperties();
        properties.getConfig().setRefreshEnabled(false);

        NacosConfigRefresher refresher = new NacosConfigRefresher(configService, properties, eventPublisher, listenersProvider);
        refresher.afterPropertiesSet();

        verifyNoInteractions(configService);
    }

    @Test
    void shouldRegisterCustomListenerWhenEnabled() throws Exception {
        ConfigService configService = mock(ConfigService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SharedConfigListener[]> listenersProvider = mock(ObjectProvider.class);

        SharedConfigListener listener = new SharedConfigListener() {
            @Override
            public String getDataIdPattern() {
                return "demo.yml";
            }

            @Override
            public String getGroup() {
                return "DEFAULT_GROUP";
            }

            @Override
            public void onChange(String dataId, String group, String content) {
            }
        };
        when(listenersProvider.getIfAvailable()).thenReturn(new SharedConfigListener[]{listener});

        NacosConfigProperties properties = new NacosConfigProperties();
        properties.getConfig().setRefreshEnabled(null);

        NacosConfigRefresher refresher = new NacosConfigRefresher(configService, properties, eventPublisher, listenersProvider);
        refresher.afterPropertiesSet();

        verify(configService).addListener(eq("demo.yml"), eq("DEFAULT_GROUP"), any());
    }
}
