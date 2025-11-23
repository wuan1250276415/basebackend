package com.basebackend.database.dynamic.config;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.dynamic.DynamicDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 动态数据源配置
 * 
 * @author basebackend
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(DatabaseEnhancedProperties.class)
@ConditionalOnProperty(
    prefix = "database.enhanced.dynamic-datasource",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class DynamicDataSourceConfig {
    
    private final DatabaseEnhancedProperties properties;
    
    public DynamicDataSourceConfig(DatabaseEnhancedProperties properties) {
        this.properties = properties;
    }
    
    /**
     * 创建动态数据源
     * 注意：这里需要在实际使用时配置具体的数据源
     */
    @Primary
    public DynamicDataSource dynamicDataSource(DataSource dataSource) {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        
        // 获取动态数据源配置
        DatabaseEnhancedProperties.DynamicDataSourceProperties dsConfig = 
            properties.getDynamicDatasource();
        
        // 设置默认数据源
        dynamicDataSource.setDefaultTargetDataSource(dataSource);
        
        // 设置主数据源键
        String primaryKey = dsConfig.getPrimary();
        if (primaryKey != null && !primaryKey.isEmpty()) {
            dynamicDataSource.setPrimaryDataSourceKey(primaryKey);
        }
        
        // 设置严格模式
        dynamicDataSource.setStrict(dsConfig.isStrict());
        
        // 初始化数据源映射
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(primaryKey != null ? primaryKey : "master", dataSource);
        
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.afterPropertiesSet();
        
        log.info("Dynamic DataSource initialized with primary: {}, strict mode: {}", 
            primaryKey, dsConfig.isStrict());
        
        return dynamicDataSource;
    }
}
