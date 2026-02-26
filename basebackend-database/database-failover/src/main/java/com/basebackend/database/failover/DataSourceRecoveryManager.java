package com.basebackend.database.failover;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.dynamic.DynamicDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源恢复管理器
 * 负责管理失败数据源的恢复检测和状态管理
 * 
 * Requirements: 6.4
 * Note: This is registered as a bean in FailoverAutoConfiguration, not auto-scanned
 * 
 * @author basebackend
 */
@Slf4j
public class DataSourceRecoveryManager {
    
    private final DatabaseEnhancedProperties properties;
    private final DataSourceFailoverHandler failoverHandler;
    private final DynamicDataSource dynamicDataSource;
    
    /**
     * 数据源映射表（用于恢复检测）
     */
    private final Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();
    
    /**
     * 从库键集合
     */
    private final Set<String> slaveKeys = ConcurrentHashMap.newKeySet();
    
    public DataSourceRecoveryManager(DatabaseEnhancedProperties properties,
                                    DataSourceFailoverHandler failoverHandler,
                                    DynamicDataSource dynamicDataSource) {
        this.properties = properties;
        this.failoverHandler = failoverHandler;
        this.dynamicDataSource = dynamicDataSource;
    }
    
    /**
     * 注册数据源用于恢复检测
     * 
     * @param key 数据源键
     * @param dataSource 数据源
     * @param isSlave 是否为从库
     */
    public void registerDataSource(String key, DataSource dataSource, boolean isSlave) {
        dataSourceMap.put(key, dataSource);
        if (isSlave) {
            slaveKeys.add(key);
            log.info("Registered slave datasource for recovery: {}", key);
        } else {
            log.info("Registered master datasource for recovery: {}", key);
        }
    }
    
    /**
     * 注销数据源
     * 
     * @param key 数据源键
     */
    public void unregisterDataSource(String key) {
        dataSourceMap.remove(key);
        slaveKeys.remove(key);
        log.info("Unregistered datasource: {}", key);
    }
    
    /**
     * 执行恢复检测
     * 检查所有失败的从库是否已恢复
     * 
     * Requirement 6.4: 从库恢复正常时自动将该节点加回可用列表
     */
    public void performRecoveryCheck() {
        if (!properties.getFailover().isEnabled()) {
            return;
        }
        
        Set<String> failedSlaves = failoverHandler.getFailedSlaves();
        if (failedSlaves.isEmpty()) {
            log.trace("No failed slaves to check for recovery");
            return;
        }
        
        log.info("Checking recovery for {} failed slave(s)", failedSlaves.size());
        
        for (String slaveKey : failedSlaves) {
            DataSource dataSource = dataSourceMap.get(slaveKey);
            if (dataSource == null) {
                log.warn("DataSource [{}] not found in registry, skipping recovery check", slaveKey);
                continue;
            }
            
            try {
                boolean recovered = failoverHandler.checkSlaveRecovery(slaveKey, dataSource);
                if (recovered) {
                    log.info("Slave [{}] successfully recovered and added back to pool", slaveKey);
                } else {
                    log.debug("Slave [{}] is still unavailable", slaveKey);
                }
            } catch (Exception e) {
                log.error("Error checking recovery for slave [{}]", slaveKey, e);
            }
        }
    }
    
    /**
     * 获取所有从库键
     * 
     * @return 从库键集合
     */
    public Set<String> getSlaveKeys() {
        return Set.copyOf(slaveKeys);
    }
    
    /**
     * 获取可用的从库键
     * 
     * @return 可用的从库键集合
     */
    public Set<String> getAvailableSlaveKeys() {
        return failoverHandler.getAvailableSlaves(slaveKeys);
    }
    
    /**
     * 检查数据源是否已注册
     * 
     * @param key 数据源键
     * @return 是否已注册
     */
    public boolean isRegistered(String key) {
        return dataSourceMap.containsKey(key);
    }
    
    /**
     * 获取已注册的数据源数量
     * 
     * @return 数据源数量
     */
    public int getRegisteredCount() {
        return dataSourceMap.size();
    }
}
