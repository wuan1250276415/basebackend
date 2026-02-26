package com.basebackend.cache.structure;

import org.redisson.api.RScoredSortedSet;

import java.util.Collection;
import java.util.Map;

/**
 * 分布式 Sorted Set 服务接口
 * 提供按分数排序的集合操作
 */
public interface DistributedSortedSetService {

    /**
     * 获取分布式 Sorted Set
     *
     * @param name Sorted Set 名称
     * @param <T>  元素类型
     * @return 分布式 Sorted Set 实例
     */
    <T> RScoredSortedSet<T> getSortedSet(String name);

    /**
     * 向 Sorted Set 中添加元素及其分数
     *
     * @param setName Sorted Set 名称
     * @param score   分数
     * @param element 元素
     * @param <T>     元素类型
     * @return 如果元素是新添加的返回 true，如果元素已存在（更新分数）返回 false
     */
    <T> boolean add(String setName, double score, T element);

    /**
     * 批量添加元素及其分数
     *
     * @param setName Sorted Set 名称
     * @param entries 元素和分数的映射
     * @param <T>     元素类型
     * @return 新添加的元素数量
     */
    <T> int addAll(String setName, Map<T, Double> entries);

    /**
     * 移除元素
     *
     * @param setName Sorted Set 名称
     * @param element 元素
     * @param <T>     元素类型
     * @return 如果移除成功返回 true
     */
    <T> boolean remove(String setName, T element);

    /**
     * 获取元素的分数
     *
     * @param setName Sorted Set 名称
     * @param element 元素
     * @param <T>     元素类型
     * @return 分数，如果元素不存在返回 null
     */
    <T> Double getScore(String setName, T element);

    /**
     * 获取元素的排名（从小到大，0 开始）
     *
     * @param setName Sorted Set 名称
     * @param element 元素
     * @param <T>     元素类型
     * @return 排名，如果元素不存在返回 null
     */
    <T> Integer rank(String setName, T element);

    /**
     * 获取元素的反向排名（从大到小，0 开始）
     *
     * @param setName Sorted Set 名称
     * @param element 元素
     * @param <T>     元素类型
     * @return 反向排名，如果元素不存在返回 null
     */
    <T> Integer revRank(String setName, T element);

    /**
     * 获取 Sorted Set 的大小
     *
     * @param setName Sorted Set 名称
     * @return 元素数量
     */
    int size(String setName);

    /**
     * 检查 Sorted Set 是否为空
     *
     * @param setName Sorted Set 名称
     * @return 如果为空返回 true
     */
    boolean isEmpty(String setName);

    /**
     * 清空 Sorted Set
     *
     * @param setName Sorted Set 名称
     */
    void clear(String setName);

    /**
     * 检查是否包含指定元素
     *
     * @param setName Sorted Set 名称
     * @param element 元素
     * @param <T>     元素类型
     * @return 如果包含返回 true
     */
    <T> boolean contains(String setName, T element);

    /**
     * 获取指定排名范围的元素（从小到大）
     *
     * @param setName   Sorted Set 名称
     * @param startRank 起始排名（包含）
     * @param endRank   结束排名（包含）
     * @param <T>       元素类型
     * @return 元素集合
     */
    <T> Collection<T> range(String setName, int startRank, int endRank);

    /**
     * 获取指定排名范围的元素（从大到小）
     *
     * @param setName   Sorted Set 名称
     * @param startRank 起始排名（包含）
     * @param endRank   结束排名（包含）
     * @param <T>       元素类型
     * @return 元素集合
     */
    <T> Collection<T> revRange(String setName, int startRank, int endRank);

    /**
     * 获取指定分数范围的元素
     *
     * @param setName    Sorted Set 名称
     * @param startScore 起始分数（包含）
     * @param endScore   结束分数（包含）
     * @param <T>        元素类型
     * @return 元素集合
     */
    <T> Collection<T> rangeByScore(String setName, double startScore, double endScore);

    /**
     * 获取指定分数范围的元素数量
     *
     * @param setName    Sorted Set 名称
     * @param startScore 起始分数（包含）
     * @param endScore   结束分数（包含）
     * @return 元素数量
     */
    int count(String setName, double startScore, double endScore);

    /**
     * 增加元素的分数
     *
     * @param setName Sorted Set 名称
     * @param element 元素
     * @param delta   增量
     * @param <T>     元素类型
     * @return 新的分数
     */
    <T> Double addScore(String setName, T element, double delta);

    /**
     * 移除指定排名范围的元素
     *
     * @param setName   Sorted Set 名称
     * @param startRank 起始排名（包含）
     * @param endRank   结束排名（包含）
     * @return 移除的元素数量
     */
    int removeRange(String setName, int startRank, int endRank);

    /**
     * 移除指定分数范围的元素
     *
     * @param setName    Sorted Set 名称
     * @param startScore 起始分数（包含）
     * @param endScore   结束分数（包含）
     * @return 移除的元素数量
     */
    int removeRangeByScore(String setName, double startScore, double endScore);

    /**
     * 获取最高分数的元素
     *
     * @param setName Sorted Set 名称
     * @param <T>     元素类型
     * @return 最高分数的元素，如果 Sorted Set 为空返回 null
     */
    <T> T first(String setName);

    /**
     * 获取最低分数的元素
     *
     * @param setName Sorted Set 名称
     * @param <T>     元素类型
     * @return 最低分数的元素，如果 Sorted Set 为空返回 null
     */
    <T> T last(String setName);
}
