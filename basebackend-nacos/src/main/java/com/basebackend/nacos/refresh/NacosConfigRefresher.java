package com.basebackend.nacos.refresh;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.basebackend.nacos.config.NacosConfigProperties;
import com.basebackend.nacos.event.ConfigChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Nacos配置刷新监听器
 * <p>
 * 自动检测是否有监听器配置，决定是否启用动态刷新功能。
 * 支持共享配置和扩展配置的监听。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(ConfigService.class)
public class NacosConfigRefresher implements InitializingBean {

    private final ConfigService configService;
    private final NacosConfigProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectProvider<SharedConfigListener[]> listenersProvider;

    private final Executor executor = Executors.newFixedThreadPool(2);

    private volatile boolean destroyed = false;

    @Override
    public void afterPropertiesSet() {
        // 获取监听器列表
        SharedConfigListener[] listenersArray = listenersProvider.getIfAvailable();
        List<SharedConfigListener> listeners = listenersArray != null ? Arrays.asList(listenersArray) : List.of();

        // 自动检测：是否启用了刷新功能
        boolean shouldEnableRefresh = properties.getConfig().getRefreshEnabled() != null
                ? properties.getConfig().getRefreshEnabled()
                : !listeners.isEmpty();

        if (!shouldEnableRefresh) {
            log.info("未配置监听器，禁用动态配置刷新");
            return;
        }

        log.info("启用动态配置刷新");

        // 注册共享配置监听器
        properties.getConfig().getSharedConfigs().forEach(sharedConfig -> {
            if (sharedConfig.isRefresh()) {
                registerListener(sharedConfig);
            }
        });

        // 注册扩展配置监听器
        properties.getConfig().getExtensionConfigs().forEach(extensionConfig -> {
            if (extensionConfig.isRefresh()) {
                registerListener(extensionConfig);
            }
        });

        // 注册自定义监听器
        listeners.forEach(listener -> {
            try {
                configService.addListener(
                        listener.getDataIdPattern(),
                        listener.getGroup(),
                        new Listener() {
                            @Override
                            public void receiveConfigInfo(String configInfo) {
                                log.info("配置变更：{}", listener.getDataIdPattern());
                                try {
                                    // 发布配置变更事件
                                    ConfigChangeEvent event = new ConfigChangeEvent(
                                            this,
                                            listener.getDataIdPattern(),
                                            listener.getGroup(),
                                            configInfo
                                    );
                                    eventPublisher.publishEvent(event);

                                    // 调用自定义监听器
                                    listener.onChange(
                                            listener.getDataIdPattern(),
                                            listener.getGroup(),
                                            configInfo
                                    );
                                } catch (Exception e) {
                                    log.error("处理配置变更事件失败", e);
                                }
                            }

                            @Override
                            public Executor getExecutor() {
                                return executor;
                            }
                        }
                );
                log.info("已注册自定义配置监听器：{}", listener.getDataIdPattern());
            } catch (Exception e) {
                log.error("注册自定义配置监听器失败：{}", listener.getDataIdPattern(), e);
            }
        });
    }

    /**
     * 注册配置监听器
     */
    private void registerListener(NacosConfigProperties.Config.SharedConfig config) {
        try {
            // 注册监听器
            configService.addListener(config.getDataId(), config.getGroup(),
                    new Listener() {
                        @Override
                        public void receiveConfigInfo(String configInfo) {
                            log.info("配置变更：{}", config.getDataId());
                            // 发布配置变更事件
                            ConfigChangeEvent event = new ConfigChangeEvent(
                                    this,
                                    config.getDataId(),
                                    config.getGroup(),
                                    configInfo
                            );
                            eventPublisher.publishEvent(event);
                        }

                        @Override
                        public Executor getExecutor() {
                            return executor;
                        }
                    }
            );

            log.info("已注册配置监听器：{}", config.getDataId());
        } catch (Exception e) {
            log.error("注册配置监听器失败：{}", config.getDataId(), e);
        }
    }

    /**
     * 注册扩展配置监听器
     */
    private void registerListener(NacosConfigProperties.Config.ExtensionConfig config) {
        try {
            // 注册监听器
            configService.addListener(config.getDataId(), config.getGroup(),
                    new Listener() {
                        @Override
                        public void receiveConfigInfo(String configInfo) {
                            log.info("扩展配置变更：{}", config.getDataId());
                            // 发布配置变更事件
                            ConfigChangeEvent event = new ConfigChangeEvent(
                                    this,
                                    config.getDataId(),
                                    config.getGroup(),
                                    configInfo
                            );
                            eventPublisher.publishEvent(event);
                        }

                        @Override
                        public Executor getExecutor() {
                            return executor;
                        }
                    }
            );

            log.info("已注册扩展配置监听器：{}", config.getDataId());
        } catch (Exception e) {
            log.error("注册扩展配置监听器失败：{}", config.getDataId(), e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (!destroyed && executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
            destroyed = true;
            log.info("Nacos配置刷新监听器已关闭，线程池已释放");
        }
    }
}

/**
 * 共享配置监听器接口
 */
interface SharedConfigListener {

    /**
     * 监听的数据ID模式
     */
    String getDataIdPattern();

    /**
     * 监听分组
     */
    String getGroup();

    /**
     * 配置变更处理
     */
    void onChange(String dataId, String group, String content);
}
