package com.basebackend.database.dynamic.service;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Set;

/**
 * 动态数据源服务接口
 * 
 * @author basebackend
 */
public interface DynamicDataSourceService {
    
    /**
     * 注册数据源
     * 
     * @param key 数据源键
     * @param dataSource 数据源
     */
    void registerDataSource(String key, DataSource dataSource);
    
    /**
     * 通过配置注册数据源
     * 
     * @param key 数据源键
     * @param url JDBC URL
     * @param username 用户名
     * @param password 密码
     */
    void registerDataSource(String key, String url, String username, String password);
    
    /**
     * 通过配置映射注册数据源
     * 
     * @param key 数据源键
     * @param properties 数据源配置属性
     */
    void registerDataSource(String key, Map<String, String> properties);
    
    /**
     * 移除数据源
     * 
     * @param key 数据源键
     * @return 是否移除成功
     */
    boolean removeDataSource(String key);
    
    /**
     * 检查数据源是否存在
     * 
     * @param key 数据源键
     * @return 是否存在
     */
    boolean containsDataSource(String key);
    
    /**
     * 获取所有数据源键
     * 
     * @return 数据源键集合
     */
    Set<Object> getAllDataSourceKeys();
    
    /**
     * 获取数据源数量
     * 
     * @return 数据源数量
     */
    int getDataSourceCount();
    
    /**
     * 测试数据源连接
     * 
     * @param key 数据源键
     * @return 是否连接成功
     */
    boolean testConnection(String key);
}
