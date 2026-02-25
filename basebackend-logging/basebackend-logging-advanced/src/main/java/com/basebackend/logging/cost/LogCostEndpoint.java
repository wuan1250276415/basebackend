package com.basebackend.logging.cost;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 日志成本治理 Actuator 端点
 *
 * 端点路径: /actuator/log-cost
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Endpoint(id = "log-cost")
public class LogCostEndpoint {

    private final LogVolumeTracker tracker;
    private final LogCostProperties properties;

    public LogCostEndpoint(LogVolumeTracker tracker, LogCostProperties properties) {
        this.tracker = tracker;
        this.properties = properties;
    }

    @ReadOperation
    public Map<String, Object> read() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", properties.isEnabled());
        result.put("windowSeconds", properties.getWindowSeconds());
        result.put("eventThreshold", properties.getEventThreshold());
        result.put("byteThreshold", properties.getByteThreshold());
        result.put("samplingRate", properties.getSamplingRate());
        result.put("exemptHighSeverity", properties.isExemptHighSeverity());
        result.put("volumes", tracker.getAllSnapshots());
        return result;
    }
}
