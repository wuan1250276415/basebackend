/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.context.ApplicationEvent
 */
package com.basebackend.nacos.event;

import com.basebackend.nacos.model.GrayReleaseHistory;
import org.springframework.context.ApplicationEvent;

public class GrayReleaseHistoryEvent
extends ApplicationEvent {
    private final GrayReleaseHistory history;

    public GrayReleaseHistoryEvent(Object source, GrayReleaseHistory history) {
        super(source);
        this.history = history;
    }

    public GrayReleaseHistory getHistory() {
        return this.history;
    }

    public String toString() {
        return "GrayReleaseHistoryEvent{dataId='" + this.history.getDataId() + "', operationType='" + this.history.getOperationType() + "', result='" + this.history.getResult() + "', operationTime=" + String.valueOf(this.history.getOperationTime()) + "}";
    }
}

