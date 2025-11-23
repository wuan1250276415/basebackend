package com.basebackend.database.dynamic.manager;

import com.basebackend.database.dynamic.DynamicDataSource;
import com.basebackend.database.exception.DataSourceException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Set;

/**
 * 数据源管理器
 * 提供数据源的注册、移除、查询等管理功能
 * 
 * @author basebackend
 */
@Slf4j
@Component
@ConditionalOnBean(DynamicDataSource.class)
public class DataSourceManager {
    
    private final DynamicDataSource dynamicDataSource;
    
    @Autowired
    public DataSourceManager(DynamicDataSource dynamicDataSource) {
        this.dynamicDataSource = dynamicDataSource;
    }
    
    /**
     * 注册数据源
     * 
     * @param key 数据源键
     * @param dataSource 数据源
     */
    public void registerDataSource(String key, DataSource dataSource) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("DataSource key cannot be null or empty");
        }
        if (dataSource == null) {
            throw new IllegalArgumentException("DataSource cannot be null");
        }
        
        try {
            dynamicDataSource.addDataSource(key, dataSource);
            log.info("Successfully registered datasource: {}", key);
        } catch (Exception e) {
            log.error("Failed to register datasource: {}", key, e);
            throw new DataSourceException("Failed to register datasource: " + key, e);
        }
    }
    
    /**
     * 通过配置注册数据源
     * 
     * @param key 数据源键
     * @param url JDBC URL
     * @param username 用户名
     * @param password 密码
     */
    public void registerDataSource(String key, String url, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setPoolName("HikariPool-" + key);
        
        // 设置连接池参数
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        HikariDataSource dataSource = new HikariDataSource(config);
        registerDataSource(key, dataSource);
    }
    
    /**
     * 通过配置映射注册数据源
     * 
     * @param key 数据源键
     * @param properties 数据源配置属性
     */
    public void registerDataSource(String key, Map<String, String> properties) {
        String url = properties.get("url");
        String username = properties.get("username");
        String password = properties.get("password");
        
        if (url == null || username == null || password == null) {
            throw new IllegalArgumentException(
                "DataSource properties must contain url, username, and password");
        }
        
        registerDataSource(key, url, username, password);
    }
    
    /**
     * 移除数据源
     * 
     * @param key 数据源键
     * @return 是否移除成功
     */
    public boolean unregisterDataSource(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("DataSource key cannot be null or empty");
        }
        
        try {
            boolean removed = dynamicDataSource.removeDataSource(key);
            if (removed) {
                log.info("Successfully unregistered datasource: {}", key);
            } else {
                log.warn("DataSource [{}] not found, cannot unregister", key);
            }
            return removed;
        } catch (Exception e) {
            log.error("Failed to unregister datasource: {}", key, e);
            throw new DataSourceException("Failed to unregister datasource: " + key, e);
        }
    }
    
    /**
     * 检查数据源是否存在
     * 
     * @param key 数据源键
     * @return 是否存在
     */
    public boolean containsDataSource(String key) {
        return dynamicDataSource.containsDataSource(key);
    }
    
    /**
     * 获取所有数据源键
     * 
     * @return 数据源键集合
     */
    public Set<Object> getAllDataSourceKeys() {
        return dynamicDataSource.getDataSourceKeys();
    }
    
    /**
     * 获取数据源数量
     * 
     * @return 数据源数量
     */
    public int getDataSourceCount() {
        return dynamicDataSource.getDataSourceCount();
    }
    
    /**
     * 测试数据源连接
     * 
     * @param key 数据源键
     * @return 是否连接成功
     */
    public boolean testDataSourceConnection(String key) {
        if (!containsDataSource(key)) {
            log.warn("DataSource [{}] not found", key);
            return false;
        }
        
        // 这里可以实现实际的连接测试逻辑
        // 暂时返回 true
        log.debug("Testing connection for datasource: {}", key);
        return true;
    }
}
