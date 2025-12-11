package com.basebackend.nacos.event;

import com.basebackend.nacos.model.GrayReleaseHistory;
import org.springframework.context.ApplicationEvent;

/**
 * 灰度发布历史事件
 * <p>
 * 当灰度发布操作完成时发布此事件，允许外部系统监听并进行持久化或其他处理
 * </p>
 */
public class GrayReleaseHistoryEvent extends ApplicationEvent {

    private final GrayReleaseHistory history;

    public GrayReleaseHistoryEvent(Object source, GrayReleaseHistory history) {
        super(source);
        this.history = history;
    }

    public GrayReleaseHistory getHistory() {
        return history;
    }

    @Override
    public String toString() {
        return "GrayReleaseHistoryEvent{" +
                "dataId='" + history.getDataId() + '\'' +
                ", operationType='" + history.getOperationType() + '\'' +
                ", result='" + history.getResult() + '\'' +
                ", operationTime=" + history.getOperationTime() +
                '}';
    }
}
