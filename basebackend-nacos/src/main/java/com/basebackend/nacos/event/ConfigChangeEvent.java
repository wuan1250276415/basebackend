package com.basebackend.nacos.event;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 配置变更事件
 */
public class ConfigChangeEvent extends ApplicationEvent {

    /**
     * 配置ID
     */
    private final String dataId;

    /**
     * 分组
     */
    private final String group;

    /**
     * 配置内容
     */
    private final String content;

    /**
     * 变更时间
     */
    private final LocalDateTime changeTime;

    /**
     * 配置源（哪个服务）
     */
    private final String sourceService;

    /**
     * 变更操作人
     */
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
        return dataId;
    }

    public String getGroup() {
        return group;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getChangeTime() {
        return changeTime;
    }

    public String getSourceService() {
        return sourceService;
    }

    public String getOperator() {
        return operator;
    }

    @Override
    public String toString() {
        return "ConfigChangeEvent{" +
                "dataId='" + dataId + '\'' +
                ", group='" + group + '\'' +
                ", changeTime=" + changeTime +
                ", sourceService='" + sourceService + '\'' +
                ", operator='" + operator + '\'' +
                '}';
    }
}
