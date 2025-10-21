package com.basebackend.nacos.listener;

import com.alibaba.nacos.api.config.listener.Listener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

import java.util.concurrent.Executor;

/**
 * 配置变更监听器
 * 监听Nacos配置变化并发布事件
 */
@Slf4j
public class ConfigChangeListener implements Listener {

    private final String dataId;
    private final String group;
    private final ApplicationEventPublisher eventPublisher;

    public ConfigChangeListener(String dataId, String group, ApplicationEventPublisher eventPublisher) {
        this.dataId = dataId;
        this.group = group;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Executor getExecutor() {
        return null; // 使用默认线程池
    }

    @Override
    public void receiveConfigInfo(String configInfo) {
        log.info("配置变更通知: dataId={}, group={}", dataId, group);

        // 发布配置变更事件
        ConfigChangeEvent event = new ConfigChangeEvent(this, dataId, group, configInfo);
        eventPublisher.publishEvent(event);
    }

    /**
     * 配置变更事件
     */
    public static class ConfigChangeEvent extends org.springframework.context.ApplicationEvent {
        private final String dataId;
        private final String group;
        private final String content;

        public ConfigChangeEvent(Object source, String dataId, String group, String content) {
            super(source);
            this.dataId = dataId;
            this.group = group;
            this.content = content;
        }

        public String getDataId() {
            return dataId;
        }

        public String getGroup() {
            return group;
        }

        public String getContent() {
            return content;
        }
    }
}
