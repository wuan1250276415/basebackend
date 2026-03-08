/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.nacos.api.config.ConfigService
 *  com.alibaba.nacos.api.config.listener.Listener
 *  jakarta.annotation.PreDestroy
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.InitializingBean
 *  org.springframework.beans.factory.ObjectProvider
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnBean
 *  org.springframework.context.ApplicationEvent
 *  org.springframework.context.ApplicationEventPublisher
 *  org.springframework.stereotype.Component
 */
package com.basebackend.nacos.refresh;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.basebackend.nacos.config.NacosConfigProperties;
import com.basebackend.nacos.event.ConfigChangeEvent;
import jakarta.annotation.PreDestroy;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(value={ConfigService.class})
public class NacosConfigRefresher
implements InitializingBean {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(NacosConfigRefresher.class);
    private final ConfigService configService;
    private final NacosConfigProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectProvider<SharedConfigListener[]> listenersProvider;
    private final Executor executor = Executors.newFixedThreadPool(2);
    private volatile boolean destroyed = false;

    public void afterPropertiesSet() {
        SharedConfigListener[] listenersArray = (SharedConfigListener[])this.listenersProvider.getIfAvailable();
        List<SharedConfigListener> listeners = listenersArray != null ? Arrays.asList(listenersArray) : List.of();
        Boolean refreshEnabled = this.properties.getConfig().getRefreshEnabled();
        boolean shouldEnableRefresh = refreshEnabled != null ? refreshEnabled : !listeners.isEmpty();
        if (!shouldEnableRefresh) {
            log.info("\u672a\u914d\u7f6e\u76d1\u542c\u5668\uff0c\u7981\u7528\u52a8\u6001\u914d\u7f6e\u5237\u65b0");
            return;
        }
        log.info("\u542f\u7528\u52a8\u6001\u914d\u7f6e\u5237\u65b0");
        this.properties.getConfig().getSharedConfigs().forEach(sharedConfig -> {
            if (sharedConfig.isRefresh()) {
                this.registerListener((NacosConfigProperties.Config.SharedConfig)sharedConfig);
            }
        });
        this.properties.getConfig().getExtensionConfigs().forEach(extensionConfig -> {
            if (extensionConfig.isRefresh()) {
                this.registerListener((NacosConfigProperties.Config.ExtensionConfig)extensionConfig);
            }
        });
        listeners.forEach(this::registerCustomListener);
    }

    private void registerCustomListener(SharedConfigListener listener) {
        try {
            this.configService.addListener(listener.getDataIdPattern(), listener.getGroup(), new Listener(){

                public void receiveConfigInfo(String configInfo) {
                    log.info("\u914d\u7f6e\u53d8\u66f4\uff1a{}", (Object)listener.getDataIdPattern());
                    try {
                        ConfigChangeEvent event = new ConfigChangeEvent(this, listener.getDataIdPattern(), listener.getGroup(), configInfo);
                        NacosConfigRefresher.this.eventPublisher.publishEvent(event);
                        listener.onChange(listener.getDataIdPattern(), listener.getGroup(), configInfo);
                    }
                    catch (Exception e) {
                        log.error("\u5904\u7406\u914d\u7f6e\u53d8\u66f4\u4e8b\u4ef6\u5931\u8d25", (Throwable)e);
                    }
                }

                public Executor getExecutor() {
                    return NacosConfigRefresher.this.executor;
                }
            });
            log.info("\u5df2\u6ce8\u518c\u81ea\u5b9a\u4e49\u914d\u7f6e\u76d1\u542c\u5668\uff1a{}", (Object)listener.getDataIdPattern());
        }
        catch (Exception e) {
            log.error("\u6ce8\u518c\u81ea\u5b9a\u4e49\u914d\u7f6e\u76d1\u542c\u5668\u5931\u8d25\uff1a{}", (Object)listener.getDataIdPattern(), (Object)e);
        }
    }

    private void registerListener(final NacosConfigProperties.Config.SharedConfig config) {
        try {
            this.configService.addListener(config.getDataId(), config.getGroup(), new Listener(){

                public void receiveConfigInfo(String configInfo) {
                    log.info("\u914d\u7f6e\u53d8\u66f4\uff1a{}", (Object)config.getDataId());
                    ConfigChangeEvent event = new ConfigChangeEvent(this, config.getDataId(), config.getGroup(), configInfo);
                    NacosConfigRefresher.this.eventPublisher.publishEvent(event);
                }

                public Executor getExecutor() {
                    return NacosConfigRefresher.this.executor;
                }
            });
            log.info("\u5df2\u6ce8\u518c\u914d\u7f6e\u76d1\u542c\u5668\uff1a{}", (Object)config.getDataId());
        }
        catch (Exception e) {
            log.error("\u6ce8\u518c\u914d\u7f6e\u76d1\u542c\u5668\u5931\u8d25\uff1a{}", (Object)config.getDataId(), (Object)e);
        }
    }

    private void registerListener(final NacosConfigProperties.Config.ExtensionConfig config) {
        try {
            this.configService.addListener(config.getDataId(), config.getGroup(), new Listener(){

                public void receiveConfigInfo(String configInfo) {
                    log.info("\u6269\u5c55\u914d\u7f6e\u53d8\u66f4\uff1a{}", (Object)config.getDataId());
                    ConfigChangeEvent event = new ConfigChangeEvent(this, config.getDataId(), config.getGroup(), configInfo);
                    NacosConfigRefresher.this.eventPublisher.publishEvent(event);
                }

                public Executor getExecutor() {
                    return NacosConfigRefresher.this.executor;
                }
            });
            log.info("\u5df2\u6ce8\u518c\u6269\u5c55\u914d\u7f6e\u76d1\u542c\u5668\uff1a{}", (Object)config.getDataId());
        }
        catch (Exception e) {
            log.error("\u6ce8\u518c\u6269\u5c55\u914d\u7f6e\u76d1\u542c\u5668\u5931\u8d25\uff1a{}", (Object)config.getDataId(), (Object)e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (!this.destroyed && this.executor instanceof ExecutorService) {
            ((ExecutorService)this.executor).shutdown();
            this.destroyed = true;
            log.info("Nacos\u914d\u7f6e\u5237\u65b0\u76d1\u542c\u5668\u5df2\u5173\u95ed\uff0c\u7ebf\u7a0b\u6c60\u5df2\u91ca\u653e");
        }
    }

    @Generated
    public NacosConfigRefresher(ConfigService configService, NacosConfigProperties properties, ApplicationEventPublisher eventPublisher, ObjectProvider<SharedConfigListener[]> listenersProvider) {
        this.configService = configService;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
        this.listenersProvider = listenersProvider;
    }
}
