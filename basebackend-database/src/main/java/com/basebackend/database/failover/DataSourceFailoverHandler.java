package com.basebackend.database.failover;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.dynamic.DynamicDataSource;
import com.basebackend.database.health.indicator.DataSourceHealthIndicator;
import com.basebackend.database.health.model.DataSourceHealth;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据源故障转移处理器
 * 负责检测数据源故障并执行故障转移策略
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5
 * Note: This is registered as a bean in FailoverAutoConfiguration, not auto-scanned
 * 
 * @author basebackend
 */
@Slf4j
public class DataSourceFailoverHandler {
    
    private final DatabaseEnhancedProperties properties;
    private final DataSourceHealthIndicator healthIndicator;
    private final DynamicDataSource dynamicDataSource;
    
    /**
     * 记录每个数据源的失败次数
     */
    private final Map<String, AtomicInteger> failureCountMap = new ConcurrentHashMap<>();
    
    /**
     * 记录已失败的从库数据源
     */
    private final Set<String> failedSlaves = ConcurrentHashMap.newKeySet();
    
    /**
     * 主库是否已降级
     */
    private volatile boolean masterDegraded = false;
    
    /**
     * 主库数据源键
     */
    private static final String MASTER_KEY = "master";
    
    public DataSourceFailoverHandler(DatabaseEnhancedProperties properties,
                                    DataSourceHealthIndicator healthIndicator,
                                    DynamicDataSource dynamicDataSource) {
        this.properties = properties;
        this.healthIndicator = healthIndicator;
        this.dynamicDataSource = dynamicDataSource;
    }
    
    /**
     * 处理主库连接失败
     * Requirement 6.1: 主库连接失败时自动尝试重连
     * 
     * @param dataSource 主库数据源
     * @return 是否重连成功
     */
    public boolean handleMasterFailure(DataSource dataSource) {
        if (!properties.getFailover().isEnabled()) {
            log.warn("Failover is disabled, skipping master failure handling");
            return false;
        }
        
        log.error("Master database connection failed, attempting reconnection...");
        
        int maxRetry = properties.getFailover().getMaxRetry();
        long retryInterval = properties.getFailover().getRetryInterval();
        
        for (int i = 1; i <= maxRetry; i++) {
            try {
                log.info("Reconnection attempt {}/{} for master database", i, maxRetry);
                
                // 等待重连间隔
                if (i > 1) {
                    Thread.sleep(retryInterval);
                }
                
                // 检查连接是否恢复
                DataSourceHealth health = healthIndicator.checkDataSource(dataSource, MASTER_KEY);
                if (health.isConnected() && health.getStatus() == DataSourceHealth.HealthStatus.UP) {
                    log.info("Master database reconnection successful");
                    resetFailureCount(MASTER_KEY);
                    masterDegraded = false;
                    return true;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Reconnection interrupted", e);
                break;
            } catch (Exception e) {
                log.error("Reconnection attempt {} failed", i, e);
            }
        }
        
        log.error("Master database reconnection failed after {} attempts", maxRetry);
        
        // Requirement 6.2: 主库持续不可用时根据配置决定是否降级
        if (properties.getFailover().isMasterDegradation()) {
            degradeMaster();
        }
        
        return false;
    }
    
    /**
     * 降级主库到只读模式
     * Requirement 6.2: 主库持续不可用时降级到只读模式
     */
    private void degradeMaster() {
        if (masterDegraded) {
            return;
        }
        
        log.warn("Degrading master database to read-only mode");
        masterDegraded = true;
        
        // 触发告警通知
        // TODO: 集成告警服务
        log.error("ALERT: Master database is degraded to read-only mode!");
    }
    
    /**
     * 处理从库连接失败
     * Requirement 6.3: 从库连接失败时自动从可用从库列表中移除
     * 
     * @param slaveKey 从库键
     */
    public void handleSlaveFailure(String slaveKey) {
        if (!properties.getFailover().isEnabled()) {
            log.warn("Failover is disabled, skipping slave failure handling");
            return;
        }
        
        log.error("Slave database [{}] connection failed", slaveKey);
        
        // 增加失败计数
        incrementFailureCount(slaveKey);
        
        // 从可用列表中移除
        if (!failedSlaves.contains(slaveKey)) {
            failedSlaves.add(slaveKey);
            log.warn("Removed slave [{}] from available pool", slaveKey);
            
            // 触发告警
            log.error("ALERT: Slave database [{}] is down!", slaveKey);
        }
    }
    
    /**
     * 检查从库是否已恢复
     * Requirement 6.4: 从库恢复正常时自动加回可用列表
     * 
     * @param slaveKey 从库键
     * @param dataSource 从库数据源
     * @return 是否已恢复
     */
    public boolean checkSlaveRecovery(String slaveKey, DataSource dataSource) {
        if (!failedSlaves.contains(slaveKey)) {
            return true; // 未失败，无需恢复
        }
        
        try {
            DataSourceHealth health = healthIndicator.checkDataSource(dataSource, slaveKey);
            if (health.isConnected() && health.getStatus() == DataSourceHealth.HealthStatus.UP) {
                log.info("Slave database [{}] has recovered", slaveKey);
                failedSlaves.remove(slaveKey);
                resetFailureCount(slaveKey);
                
                log.info("Added slave [{}] back to available pool", slaveKey);
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to check slave [{}] recovery", slaveKey, e);
        }
        
        return false;
    }
    
    /**
     * 获取可用的从库列表
     * Requirement 6.5: 所有从库不可用时将读请求路由到主库
     * 
     * @param allSlaves 所有从库键
     * @return 可用的从库键集合
     */
    public Set<String> getAvailableSlaves(Set<String> allSlaves) {
        Set<String> available = ConcurrentHashMap.newKeySet();
        
        for (String slave : allSlaves) {
            if (!failedSlaves.contains(slave)) {
                available.add(slave);
            }
        }
        
        // Requirement 6.5: 如果所有从库都不可用，返回空集合，调用方应路由到主库
        if (available.isEmpty() && !allSlaves.isEmpty()) {
            log.warn("All slave databases are unavailable, reads will be routed to master");
        }
        
        return available;
    }
    
    /**
     * 检查主库是否已降级
     * 
     * @return 是否已降级
     */
    public boolean isMasterDegraded() {
        return masterDegraded;
    }
    
    /**
     * 获取失败的从库集合
     * 
     * @return 失败的从库键集合
     */
    public Set<String> getFailedSlaves() {
        return Set.copyOf(failedSlaves);
    }
    
    /**
     * 增加失败计数
     */
    private void incrementFailureCount(String dataSourceKey) {
        failureCountMap.computeIfAbsent(dataSourceKey, k -> new AtomicInteger(0))
                      .incrementAndGet();
    }
    
    /**
     * 重置失败计数
     */
    private void resetFailureCount(String dataSourceKey) {
        failureCountMap.remove(dataSourceKey);
    }
    
    /**
     * 获取失败计数
     */
    public int getFailureCount(String dataSourceKey) {
        AtomicInteger count = failureCountMap.get(dataSourceKey);
        return count != null ? count.get() : 0;
    }
    
    /**
     * 重置所有状态（用于测试）
     */
    public void reset() {
        failureCountMap.clear();
        failedSlaves.clear();
        masterDegraded = false;
    }
}
