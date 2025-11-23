package com.basebackend.backup.infrastructure.executor;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL WAL事件列表
 */
@Data
@NoArgsConstructor
public class WalEventList {

    private List<WalEvent> events = new ArrayList<>();

    /**
     * 添加事件
     */
    public void addEvent(WalEvent event) {
        if (event != null) {
            events.add(event);
        }
    }

    /**
     * 获取事件数量
     */
    public int getEventCount() {
        return events.size();
    }

    /**
     * 获取所有事件
     */
    public List<WalEvent> getAllEvents() {
        return new ArrayList<>(events);
    }

    /**
     * 按操作类型过滤事件
     */
    public List<WalEvent> filterByOperation(String operation) {
        return events.stream()
            .filter(event -> operation.equals(event.getOperation()))
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 按数据库过滤事件
     */
    public List<WalEvent> filterByDatabase(String database) {
        return events.stream()
            .filter(event -> database.equals(event.getDatabase()))
            .collect(java.util.stream.Collectors.toList());
    }
}
