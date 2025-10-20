package com.basebackend.admin.service.messaging;

import com.basebackend.admin.mapper.messaging.SysMessageLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息监控服务
 */
@Slf4j
@Service
public class MessageMonitorService {

    private final SysMessageLogMapper messageLogMapper;

    public MessageMonitorService(SysMessageLogMapper messageLogMapper) {
        this.messageLogMapper = messageLogMapper;
    }

    /**
     * 获取消息统计信息
     */
    public Map<String, Object> getMessageStatistics() {
        Map<String, Object> stats = messageLogMapper.getMessageStats();

        // 计算成功率
        long total = ((Number) stats.getOrDefault("total", 0L)).longValue();
        long consumed = ((Number) stats.getOrDefault("consumed", 0L)).longValue();
        double successRate = total > 0 ? (consumed * 100.0 / total) : 0.0;

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("pending", stats.get("pending"));
        result.put("sent", stats.get("sent"));
        result.put("consumed", consumed);
        result.put("failed", stats.get("failed"));
        result.put("deadLetter", stats.get("deadLetter"));
        result.put("successRate", String.format("%.2f%%", successRate));

        return result;
    }

    /**
     * 获取队列监控信息（模拟数据，实际应该从RabbitMQ Management API获取）
     */
    public Map<String, Object> getQueueMonitor() {
        Map<String, Object> monitor = new HashMap<>();

        // 这里应该调用RabbitMQ Management API获取实际数据
        // 示例数据
        monitor.put("queueCount", 5);
        monitor.put("totalMessages", 120);
        monitor.put("readyMessages", 80);
        monitor.put("unackedMessages", 40);
        monitor.put("consumerCount", 10);
        monitor.put("messageRate", 15.5);
        monitor.put("ackRate", 14.8);

        return monitor;
    }
}
