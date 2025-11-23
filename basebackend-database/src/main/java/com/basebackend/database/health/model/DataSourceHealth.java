package com.basebackend.database.health.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据源健康状态模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceHealth {
    
    /**
     * 数据源名称
     */
    private String name;
    
    /**
     * 是否连接
     */
    private boolean connected;
    
    /**
     * 响应时间 (ms)
     */
    private long responseTime;
    
    /**
     * 活跃连接数
     */
    private int activeConnections;
    
    /**
     * 空闲连接数
     */
    private int idleConnections;
    
    /**
     * 最大连接数
     */
    private int maxConnections;
    
    /**
     * 连接池使用率 (0-100)
     */
    private double poolUsageRate;
    
    /**
     * 最后检查时间
     */
    private LocalDateTime lastCheckTime;
    
    /**
     * 错误信息（如果有）
     */
    private String errorMessage;
    
    /**
     * 数据源类型 (master/slave/tenant)
     */
    private String type;
    
    /**
     * 健康状态 (UP/DOWN/DEGRADED)
     */
    private HealthStatus status;
    
    /**
     * 健康状态枚举
     */
    public enum HealthStatus {
        UP,
        DOWN,
        DEGRADED
    }
}
