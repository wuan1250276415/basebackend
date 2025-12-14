package com.basebackend.database.dynamic.service.impl;

import com.basebackend.database.dynamic.manager.DataSourceManager;
import com.basebackend.database.dynamic.service.DynamicDataSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Set;

/**
 * 动态数据源服务实现
 * 
 * @author basebackend
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "database.enhanced.dynamic-datasource", name = "enabled", havingValue = "true", matchIfMissing = false)
public class DynamicDataSourceServiceImpl implements DynamicDataSourceService {

    private final DataSourceManager dataSourceManager;

    @Autowired
    public DynamicDataSourceServiceImpl(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    @Override
    public void registerDataSource(String key, DataSource dataSource) {
        log.info("Registering datasource: {}", key);
        dataSourceManager.registerDataSource(key, dataSource);
    }

    @Override
    public void registerDataSource(String key, String url, String username, String password) {
        log.info("Registering datasource: {} with url: {}", key, url);
        dataSourceManager.registerDataSource(key, url, username, password);
    }

    @Override
    public void registerDataSource(String key, Map<String, String> properties) {
        log.info("Registering datasource: {} with properties", key);
        dataSourceManager.registerDataSource(key, properties);
    }

    @Override
    public boolean removeDataSource(String key) {
        log.info("Removing datasource: {}", key);
        return dataSourceManager.unregisterDataSource(key);
    }

    @Override
    public boolean containsDataSource(String key) {
        return dataSourceManager.containsDataSource(key);
    }

    @Override
    public Set<Object> getAllDataSourceKeys() {
        return dataSourceManager.getAllDataSourceKeys();
    }

    @Override
    public int getDataSourceCount() {
        return dataSourceManager.getDataSourceCount();
    }

    @Override
    public boolean testConnection(String key) {
        log.debug("Testing connection for datasource: {}", key);
        return dataSourceManager.testDataSourceConnection(key);
    }
}
