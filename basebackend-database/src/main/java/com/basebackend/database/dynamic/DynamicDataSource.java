package com.basebackend.database.dynamic;

import com.basebackend.database.dynamic.context.DataSourceContextHolder;
import com.basebackend.database.exception.DataSourceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态数据源
 * 支持运行时数据源切换和动态添加/移除数据源
 * 
 * @author basebackend
 */
@Slf4j
public class DynamicDataSource extends AbstractRoutingDataSource {
    
    /**
     * 数据源映射表
     */
    private final Map<Object, Object> targetDataSourceMap = new ConcurrentHashMap<>();
    
    /**
     * 默认数据源键
     */
    private String primaryDataSourceKey = "master";
    
    /**
     * 是否严格模式（数据源不存在时抛异常）
     */
    private boolean strict = true;
    
    public DynamicDataSource() {
        super();
    }
    
    /**
     * 设置默认数据源键
     */
    public void setPrimaryDataSourceKey(String primaryDataSourceKey) {
        this.primaryDataSourceKey = primaryDataSourceKey;
    }
    
    /**
     * 设置严格模式
     */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }
    
    /**
     * 初始化数据源映射
     */
    @Override
    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
        super.setTargetDataSources(targetDataSources);
        this.targetDataSourceMap.putAll(targetDataSources);
    }
    
    /**
     * 确定当前使用的数据源键
     * 
     * @return 数据源键
     */
    @Override
    protected Object determineCurrentLookupKey() {
        String dataSourceKey = DataSourceContextHolder.getDataSourceKey();
        
        // 如果没有设置数据源，使用默认数据源
        if (dataSourceKey == null) {
            log.trace("No datasource key in context, using primary: {}", primaryDataSourceKey);
            return primaryDataSourceKey;
        }
        
        // 严格模式下，检查数据源是否存在
        if (strict && !targetDataSourceMap.containsKey(dataSourceKey)) {
            throw new DataSourceException(
                String.format("DataSource [%s] not found. Available datasources: %s", 
                    dataSourceKey, targetDataSourceMap.keySet()), null);
        }
        
        log.trace("Using datasource: {}", dataSourceKey);
        return dataSourceKey;
    }
    
    /**
     * 动态添加数据源
     * 
     * @param key 数据源键
     * @param dataSource 数据源
     */
    public void addDataSource(String key, DataSource dataSource) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("DataSource key cannot be null or empty");
        }
        if (dataSource == null) {
            throw new IllegalArgumentException("DataSource cannot be null");
        }
        
        targetDataSourceMap.put(key, dataSource);
        super.setTargetDataSources(targetDataSourceMap);
        super.afterPropertiesSet();
        
        log.info("Added datasource: {}", key);
    }
    
    /**
     * 动态移除数据源
     * 
     * @param key 数据源键
     * @return 是否移除成功
     */
    public boolean removeDataSource(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("DataSource key cannot be null or empty");
        }
        
        // 不允许移除主数据源
        if (primaryDataSourceKey.equals(key)) {
            throw new DataSourceException(
                String.format("Cannot remove primary datasource: %s", key), null);
        }
        
        Object removed = targetDataSourceMap.remove(key);
        if (removed != null) {
            super.setTargetDataSources(targetDataSourceMap);
            super.afterPropertiesSet();
            log.info("Removed datasource: {}", key);
            return true;
        }
        
        log.warn("DataSource [{}] not found, cannot remove", key);
        return false;
    }
    
    /**
     * 检查数据源是否存在
     * 
     * @param key 数据源键
     * @return 是否存在
     */
    public boolean containsDataSource(String key) {
        return targetDataSourceMap.containsKey(key);
    }
    
    /**
     * 获取所有数据源键
     * 
     * @return 数据源键集合
     */
    public java.util.Set<Object> getDataSourceKeys() {
        return targetDataSourceMap.keySet();
    }
    
    /**
     * 获取数据源数量
     * 
     * @return 数据源数量
     */
    public int getDataSourceCount() {
        return targetDataSourceMap.size();
    }
}
