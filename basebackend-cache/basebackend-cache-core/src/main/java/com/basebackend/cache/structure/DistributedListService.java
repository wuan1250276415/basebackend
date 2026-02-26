package com.basebackend.cache.structure;

import org.redisson.api.RList;

import java.util.Collection;
import java.util.List;

/**
 * 分布式 List 服务接口
 * 提供有序列表操作
 */
public interface DistributedListService {

    /**
     * 获取分布式 List
     *
     * @param name List 名称
     * @param <T>  元素类型
     * @return 分布式 List 实例
     */
    <T> RList<T> getList(String name);

    /**
     * 向 List 尾部添加元素
     *
     * @param listName List 名称
     * @param element  元素
     * @param <T>      元素类型
     * @return 如果添加成功返回 true
     */
    <T> boolean add(String listName, T element);

    /**
     * 在指定位置插入元素
     *
     * @param listName List 名称
     * @param index    索引位置
     * @param element  元素
     * @param <T>      元素类型
     */
    <T> void add(String listName, int index, T element);

    /**
     * 批量添加元素
     *
     * @param listName List 名称
     * @param elements 元素集合
     * @param <T>      元素类型
     * @return 如果至少添加了一个元素返回 true
     */
    <T> boolean addAll(String listName, Collection<T> elements);

    /**
     * 获取指定位置的元素
     *
     * @param listName List 名称
     * @param index    索引位置
     * @param <T>      元素类型
     * @return 元素
     */
    <T> T get(String listName, int index);

    /**
     * 设置指定位置的元素
     *
     * @param listName List 名称
     * @param index    索引位置
     * @param element  元素
     * @param <T>      元素类型
     * @return 原来位置上的元素
     */
    <T> T set(String listName, int index, T element);

    /**
     * 移除指定位置的元素
     *
     * @param listName List 名称
     * @param index    索引位置
     * @param <T>      元素类型
     * @return 被移除的元素
     */
    <T> T remove(String listName, int index);

    /**
     * 移除指定元素（第一次出现）
     *
     * @param listName List 名称
     * @param element  元素
     * @param <T>      元素类型
     * @return 如果移除成功返回 true
     */
    <T> boolean remove(String listName, T element);

    /**
     * 获取 List 的大小
     *
     * @param listName List 名称
     * @return List 中元素的数量
     */
    int size(String listName);

    /**
     * 检查 List 是否为空
     *
     * @param listName List 名称
     * @return 如果 List 为空返回 true
     */
    boolean isEmpty(String listName);

    /**
     * 清空 List
     *
     * @param listName List 名称
     */
    void clear(String listName);

    /**
     * 检查 List 是否包含指定元素
     *
     * @param listName List 名称
     * @param element  元素
     * @param <T>      元素类型
     * @return 如果包含返回 true
     */
    <T> boolean contains(String listName, T element);

    /**
     * 获取元素在 List 中的索引位置
     *
     * @param listName List 名称
     * @param element  元素
     * @param <T>      元素类型
     * @return 索引位置，如果不存在返回 -1
     */
    <T> int indexOf(String listName, T element);

    /**
     * 获取 List 中所有元素
     *
     * @param listName List 名称
     * @param <T>      元素类型
     * @return 元素列表
     */
    <T> List<T> readAll(String listName);

    /**
     * 获取指定范围的元素
     *
     * @param listName  List 名称
     * @param fromIndex 起始索引（包含）
     * @param toIndex   结束索引（包含）
     * @param <T>       元素类型
     * @return 元素列表
     */
    <T> List<T> range(String listName, int fromIndex, int toIndex);

    /**
     * 移除并返回第一个元素
     *
     * @param listName List 名称
     * @param <T>      元素类型
     * @return 第一个元素，如果 List 为空返回 null
     */
    <T> T removeFirst(String listName);

    /**
     * 移除并返回最后一个元素
     *
     * @param listName List 名称
     * @param <T>      元素类型
     * @return 最后一个元素，如果 List 为空返回 null
     */
    <T> T removeLast(String listName);
}
