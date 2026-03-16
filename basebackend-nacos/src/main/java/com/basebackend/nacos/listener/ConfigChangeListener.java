/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.nacos.api.config.listener.Listener
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.context.ApplicationEvent
 *  org.springframework.context.ApplicationEventPublisher
 */
package com.basebackend.nacos.listener;

import com.alibaba.nacos.api.config.listener.Listener;
import java.util.concurrent.Executor;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

public class ConfigChangeListener
implements Listener {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(ConfigChangeListener.class);
    private final String dataId;
    private final String group;
    private final ApplicationEventPublisher eventPublisher;

    public ConfigChangeListener(String dataId, String group, ApplicationEventPublisher eventPublisher) {
        this.dataId = dataId;
        this.group = group;
        this.eventPublisher = eventPublisher;
    }

    public Executor getExecutor() {
        return null;
    }

    public void receiveConfigInfo(String configInfo) {
        log.info("\u914d\u7f6e\u53d8\u66f4\u901a\u77e5: dataId={}, group={}", (Object)this.dataId, (Object)this.group);
        ConfigChangeEvent event = new ConfigChangeEvent(this, this.dataId, this.group, configInfo);
        this.eventPublisher.publishEvent((ApplicationEvent)event);
    }

    public static class ConfigChangeEvent
    extends ApplicationEvent {
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
            return this.dataId;
        }

        public String getGroup() {
            return this.group;
        }

        public String getContent() {
            return this.content;
        }
    }
}

