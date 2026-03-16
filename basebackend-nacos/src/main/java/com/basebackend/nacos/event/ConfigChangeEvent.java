/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.context.ApplicationEvent
 */
package com.basebackend.nacos.event;

import java.time.LocalDateTime;
import org.springframework.context.ApplicationEvent;

public class ConfigChangeEvent
extends ApplicationEvent {
    private final String dataId;
    private final String group;
    private final String content;
    private final LocalDateTime changeTime;
    private final String sourceService;
    private final String operator;

    public ConfigChangeEvent(Object source, String dataId, String group, String content) {
        super(source);
        this.dataId = dataId;
        this.group = group;
        this.content = content;
        this.changeTime = LocalDateTime.now();
        this.sourceService = null;
        this.operator = null;
    }

    public ConfigChangeEvent(Object source, String dataId, String group, String content, String sourceService, String operator) {
        super(source);
        this.dataId = dataId;
        this.group = group;
        this.content = content;
        this.changeTime = LocalDateTime.now();
        this.sourceService = sourceService;
        this.operator = operator;
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

    public LocalDateTime getChangeTime() {
        return this.changeTime;
    }

    public String getSourceService() {
        return this.sourceService;
    }

    public String getOperator() {
        return this.operator;
    }

    public String toString() {
        return "ConfigChangeEvent{dataId='" + this.dataId + "', group='" + this.group + "', changeTime=" + String.valueOf(this.changeTime) + ", sourceService='" + this.sourceService + "', operator='" + this.operator + "'}";
    }
}

