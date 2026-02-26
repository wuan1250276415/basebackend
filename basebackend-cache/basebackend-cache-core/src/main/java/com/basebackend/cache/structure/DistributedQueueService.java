package com.basebackend.cache.structure;

import org.redisson.api.RQueue;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 分布式队列服务接口
 * 提供 FIFO 队列操作
 */
public interface DistributedQueueService {

    /**
     * 获取分布式队列
     *
     * @param name 队列名称
     * @param <T>  元素类型
     * @return 分布式队列实例
     */
    <T> RQueue<T> getQueue(String name);

    /**
     * 向队列尾部添加元素
     *
     * @param queueName 队列名称
     * @param element   元素
     * @param <T>       元素类型
     * @return 如果添加成功返回 true
     */
    <T> boolean offer(String queueName, T element);

    /**
     * 从队列头部移除并返回元素
     *
     * @param queueName 队列名称
     * @param <T>       元素类型
     * @return 队列头部的元素，如果队列为空则返回 null
     */
    <T> T poll(String queueName);

    /**
     * 查看队列头部元素但不移除
     *
     * @param queueName 队列名称
     * @param <T>       元素类型
     * @return 队列头部的元素，如果队列为空则返回 null
     */
    <T> T peek(String queueName);

    /**
     * 获取队列大小
     *
     * @param queueName 队列名称
     * @return 队列中元素的数量
     */
    long size(String queueName);

    /**
     * 检查队列是否为空
     *
     * @param queueName 队列名称
     * @return 如果队列为空返回 true
     */
    boolean isEmpty(String queueName);

    /**
     * 清空队列
     *
     * @param queueName 队列名称
     */
    void clear(String queueName);

    /**
     * 批量添加元素
     *
     * @param queueName 队列名称
     * @param elements  元素集合
     * @param <T>       元素类型
     * @return 如果添加成功返回 true
     */
    <T> boolean addAll(String queueName, Collection<T> elements);

    /**
     * 移除指定元素
     *
     * @param queueName 队列名称
     * @param element   要移除的元素
     * @param <T>       元素类型
     * @return 如果移除成功返回 true
     */
    <T> boolean remove(String queueName, T element);

    /**
     * 检查队列是否包含指定元素
     *
     * @param queueName 队列名称
     * @param element   元素
     * @param <T>       元素类型
     * @return 如果包含返回 true
     */
    <T> boolean contains(String queueName, T element);

    /**
     * 获取队列中所有元素（不移除）
     *
     * @param queueName 队列名称
     * @param <T>       元素类型
     * @return 元素列表
     */
    <T> List<T> readAll(String queueName);
}
