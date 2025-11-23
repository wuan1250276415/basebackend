package com.basebackend.cache.service;

import com.basebackend.cache.metrics.CacheStatistics;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 统一缓存服务接口
 * 提供完整的缓存操作 API，整合多级缓存、指标收集、生命周期管理等功能
 * 
 * 这是应用层使用缓存的主要入口
 */
public interface CacheService {

    // ========== 基本缓存操作 ==========

    /**
     * 获取缓存
     * 
     * @param key 缓存键
     * @param type 值类型
     * @return 缓存值，如果不存在返回 null
     */
    <T> T get(String key, Class<T> type);

    /**
     * 设置缓存
     * 
     * @param key 缓存键
     * @param value 缓存值
     */
    void set(String key, Object value);

    /**
     * 设置缓存并指定过期时间
     * 
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 过期时间
     */
    void set(String key, Object value, Duration ttl);

    /**
     * 删除缓存
     * 
     * @param key 缓存键
     * @return true 如果成功删除
     */
    boolean delete(String key);

    /**
     * 检查缓存是否存在
     * 
     * @param key 缓存键
     * @return true 如果存在
     */
    boolean exists(String key);

    // ========== Cache-Aside 模式 ==========

    /**
     * Cache-Aside 模式：获取缓存，未命中时从数据源加载
     * 
     * @param key 缓存键
     * @param loader 数据加载函数
     * @param ttl 过期时间
     * @return 缓存值或加载的值
     */
    <T> T getOrLoad(String key, Supplier<T> loader, Duration ttl);

    /**
     * Cache-Aside 模式：获取缓存，未命中时从数据源加载（使用默认 TTL）
     * 
     * @param key 缓存键
     * @param loader 数据加载函数
     * @return 缓存值或加载的值
     */
    <T> T getOrLoad(String key, Supplier<T> loader);

    // ========== 批量操作 ==========

    /**
     * 批量获取缓存
     * 
     * @param keys 键集合
     * @param type 值类型
     * @return 键值对映射
     */
    <T> Map<String, T> multiGet(Set<String> keys, Class<T> type);

    /**
     * 批量设置缓存
     * 
     * @param entries 键值对映射
     * @param ttl 过期时间
     */
    void multiSet(Map<String, Object> entries, Duration ttl);

    /**
     * 批量设置缓存（不设置过期时间）
     * 
     * @param entries 键值对映射
     */
    void multiSet(Map<String, Object> entries);

    /**
     * 批量删除缓存
     * 
     * @param keys 键集合
     * @return 删除的键数量
     */
    long multiDelete(Set<String> keys);

    // ========== 模式匹配操作 ==========

    /**
     * 模式匹配删除
     * 删除所有匹配指定模式的键
     * 
     * @param pattern 键模式（支持通配符 * 和 ?）
     * @return 删除的键数量
     */
    long deleteByPattern(String pattern);

    /**
     * 获取所有匹配模式的键
     * 
     * @param pattern 键模式
     * @return 匹配的键集合
     */
    Set<String> keys(String pattern);

    // ========== 缓存生命周期管理 ==========

    /**
     * 创建缓存（验证名称和配置）
     * 
     * @param cacheName 缓存名称
     * @return true 如果成功创建
     */
    boolean createCache(String cacheName);

    /**
     * 验证缓存名称的有效性
     * 
     * @param cacheName 缓存名称
     * @return true 如果有效
     */
    boolean validateCacheName(String cacheName);

    /**
     * 清空指定缓存
     * 
     * @param cacheName 缓存名称
     * @return 删除的键数量
     */
    long clearCache(String cacheName);

    /**
     * 清空所有缓存
     * 
     * @return 删除的键数量
     */
    long clearAllCaches();

    // ========== 缓存统计和监控 ==========

    /**
     * 获取缓存统计信息
     * 
     * @param cacheName 缓存名称
     * @return 统计信息
     */
    CacheStatistics getStatistics(String cacheName);

    /**
     * 获取缓存大小
     * 
     * @param cacheName 缓存名称
     * @return 缓存条目数
     */
    long getCacheSize(String cacheName);

    /**
     * 获取所有缓存名称
     * 
     * @return 缓存名称集合
     */
    Set<String> getAllCacheNames();

    /**
     * 重置缓存统计信息
     * 
     * @param cacheName 缓存名称
     */
    void resetStatistics(String cacheName);

    // ========== 过期时间管理 ==========

    /**
     * 获取键的过期时间
     * 
     * @param key 缓存键
     * @return 过期时间（秒），-1 表示永不过期，-2 表示键不存在
     */
    long getExpiration(String key);

    /**
     * 设置键的过期时间
     * 
     * @param key 缓存键
     * @param duration 过期时间
     * @return true 如果成功设置
     */
    boolean setExpiration(String key, Duration duration);

    /**
     * 移除键的过期时间（设置为永不过期）
     * 
     * @param key 缓存键
     * @return true 如果成功移除
     */
    boolean removeExpiration(String key);

    // ========== 容量管理 ==========

    /**
     * 检查并执行容量管理
     * 如果缓存大小超过限制，根据淘汰策略清理数据
     * 
     * @return 淘汰的键数量
     */
    long enforceCapacity();

    /**
     * 设置缓存容量限制
     * 
     * @param maxSize 最大容量（-1 表示无限制）
     */
    void setMaxCacheSize(long maxSize);

    /**
     * 获取缓存容量限制
     * 
     * @return 最大容量
     */
    long getMaxCacheSize();
}
