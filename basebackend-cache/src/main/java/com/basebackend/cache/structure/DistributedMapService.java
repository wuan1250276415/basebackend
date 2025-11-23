package com.basebackend.cache.structure;

import org.redisson.api.RMap;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 分布式 Map 服务接口
 * 提供线程安全的分布式键值对存储
 */
public interface DistributedMapService {

    /**
     * 获取分布式 Map
     *
     * @param name Map 名称
     * @param <K>  键类型
     * @param <V>  值类型
     * @return 分布式 Map 实例
     */
    <K, V> RMap<K, V> getMap(String name);

    /**
     * 向 Map 中添加键值对
     *
     * @param mapName Map 名称
     * @param key     键
     * @param value   值
     * @param <K>     键类型
     * @param <V>     值类型
     */
    <K, V> void put(String mapName, K key, V value);

    /**
     * 向 Map 中添加键值对，并设置过期时间
     *
     * @param mapName  Map 名称
     * @param key      键
     * @param value    值
     * @param ttl      过期时间
     * @param timeUnit 时间单位
     * @param <K>      键类型
     * @param <V>      值类型
     */
    <K, V> void put(String mapName, K key, V value, long ttl, TimeUnit timeUnit);

    /**
     * 从 Map 中获取值
     *
     * @param mapName Map 名称
     * @param key     键
     * @param <K>     键类型
     * @param <V>     值类型
     * @return 值，如果不存在则返回 null
     */
    <K, V> V get(String mapName, K key);

    /**
     * 从 Map 中删除键值对
     *
     * @param mapName Map 名称
     * @param key     键
     * @param <K>     键类型
     */
    <K> void remove(String mapName, K key);

    /**
     * 检查 Map 中是否包含指定的键
     *
     * @param mapName Map 名称
     * @param key     键
     * @param <K>     键类型
     * @return 如果包含则返回 true，否则返回 false
     */
    <K> boolean containsKey(String mapName, K key);

    /**
     * 获取 Map 的大小
     *
     * @param mapName Map 名称
     * @return Map 中键值对的数量
     */
    int size(String mapName);

    /**
     * 清空 Map
     *
     * @param mapName Map 名称
     */
    void clear(String mapName);

    /**
     * 获取 Map 中所有的键
     *
     * @param mapName Map 名称
     * @param <K>     键类型
     * @return 键的集合
     */
    <K> Set<K> keySet(String mapName);

    /**
     * 批量添加键值对
     *
     * @param mapName Map 名称
     * @param entries 键值对集合
     * @param <K>     键类型
     * @param <V>     值类型
     */
    <K, V> void putAll(String mapName, Map<K, V> entries);

    /**
     * 如果键不存在则添加
     *
     * @param mapName Map 名称
     * @param key     键
     * @param value   值
     * @param <K>     键类型
     * @param <V>     值类型
     * @return 如果添加成功返回 true，如果键已存在返回 false
     */
    <K, V> boolean putIfAbsent(String mapName, K key, V value);
}
