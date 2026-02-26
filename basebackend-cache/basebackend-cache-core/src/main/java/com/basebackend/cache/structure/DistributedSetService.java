package com.basebackend.cache.structure;

import org.redisson.api.RSet;

import java.util.Collection;
import java.util.Set;

/**
 * 分布式 Set 服务接口
 * 提供去重的集合操作
 */
public interface DistributedSetService {

    /**
     * 获取分布式 Set
     *
     * @param name Set 名称
     * @param <T>  元素类型
     * @return 分布式 Set 实例
     */
    <T> RSet<T> getSet(String name);

    /**
     * 向 Set 中添加元素
     *
     * @param setName Set 名称
     * @param element 元素
     * @param <T>     元素类型
     * @return 如果元素不存在并添加成功返回 true，如果元素已存在返回 false
     */
    <T> boolean add(String setName, T element);

    /**
     * 批量添加元素
     *
     * @param setName  Set 名称
     * @param elements 元素集合
     * @param <T>      元素类型
     * @return 如果至少添加了一个元素返回 true
     */
    <T> boolean addAll(String setName, Collection<T> elements);

    /**
     * 从 Set 中移除元素
     *
     * @param setName Set 名称
     * @param element 元素
     * @param <T>     元素类型
     * @return 如果元素存在并移除成功返回 true
     */
    <T> boolean remove(String setName, T element);

    /**
     * 检查 Set 是否包含指定元素
     *
     * @param setName Set 名称
     * @param element 元素
     * @param <T>     元素类型
     * @return 如果包含返回 true
     */
    <T> boolean contains(String setName, T element);

    /**
     * 获取 Set 的大小
     *
     * @param setName Set 名称
     * @return Set 中元素的数量
     */
    int size(String setName);

    /**
     * 检查 Set 是否为空
     *
     * @param setName Set 名称
     * @return 如果 Set 为空返回 true
     */
    boolean isEmpty(String setName);

    /**
     * 清空 Set
     *
     * @param setName Set 名称
     */
    void clear(String setName);

    /**
     * 获取 Set 中所有元素
     *
     * @param setName Set 名称
     * @param <T>     元素类型
     * @return 元素集合
     */
    <T> Set<T> readAll(String setName);

    /**
     * 随机移除并返回一个元素
     *
     * @param setName Set 名称
     * @param <T>     元素类型
     * @return 随机元素，如果 Set 为空返回 null
     */
    <T> T removeRandom(String setName);

    /**
     * 随机获取一个元素但不移除
     *
     * @param setName Set 名称
     * @param <T>     元素类型
     * @return 随机元素，如果 Set 为空返回 null
     */
    <T> T random(String setName);

    /**
     * 随机获取多个元素但不移除
     *
     * @param setName Set 名称
     * @param count   数量
     * @param <T>     元素类型
     * @return 随机元素集合
     */
    <T> Set<T> random(String setName, int count);

    /**
     * 计算两个 Set 的交集
     *
     * @param setName1 第一个 Set 名称
     * @param setName2 第二个 Set 名称
     * @param <T>      元素类型
     * @return 交集元素集合
     */
    <T> Set<T> intersection(String setName1, String setName2);

    /**
     * 计算两个 Set 的并集
     *
     * @param setName1 第一个 Set 名称
     * @param setName2 第二个 Set 名称
     * @param <T>      元素类型
     * @return 并集元素集合
     */
    <T> Set<T> union(String setName1, String setName2);

    /**
     * 计算两个 Set 的差集
     *
     * @param setName1 第一个 Set 名称
     * @param setName2 第二个 Set 名称
     * @param <T>      元素类型
     * @return 差集元素集合（在 setName1 中但不在 setName2 中的元素）
     */
    <T> Set<T> difference(String setName1, String setName2);
}
