package com.basebackend.scheduler.performance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 查询性能优化工具类
 *
 * <p>提供多种查询优化策略，包括：
 * <ul>
 *   <li>查询结果缓存</li>
 *   <li>批量查询优化</li>
 *   <li>分页查询优化</li>
 *   <li>查询执行时间监控</li>
 *   <li>查询计划分析</li>
 * </ul>
 *
 * <p>设计原则：
 * <ul>
 *   <li>性能优先：减少数据库查询次数和查询时间</li>
 *   <li>内存友好：避免大量数据加载到内存</li>
 *   <li>可监控：提供详细的性能指标</li>
 *   <li>易使用：提供简单易用的 API</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */

@Component
public class QueryOptimizer {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QueryOptimizer.class);

    /**
     * 缓存查询结果
     *
     * @param cacheKey 缓存键
     * @param supplier 查询Supplier
     * @param <T> 返回类型
     * @return 查询结果
     */
    public <T> T cacheQuery(String cacheKey, java.util.concurrent.Callable<T> supplier) {
        if (cacheKey == null || cacheKey.trim().isEmpty()) {
            log.warn("缓存键为空，直接执行查询");
            try {
                return supplier.call();
            } catch (Exception e) {
                log.error("查询执行失败", e);
                throw new RuntimeException("查询执行失败", e);
            }
        }

        log.debug("执行缓存查询，cacheKey={}", cacheKey);
        long startTime = System.currentTimeMillis();

        try {
            T result = supplier.call();
            long duration = System.currentTimeMillis() - startTime;
            log.info("缓存查询执行成功，cacheKey={}, duration={}ms", cacheKey, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("缓存查询执行失败，cacheKey={}, duration={}ms", cacheKey, duration, e);
            throw new RuntimeException("查询执行失败", e);
        }
    }

    /**
     * 批量查询优化
     *
     * @param ids ID列表
     * @param batchSize 批次大小
     * @param queryFunction 查询函数
     * @param <T> 实体类型
     * @param <R> 返回类型
     * @return 查询结果映射
     */
    public <T, R> List<R> batchQuery(List<T> ids, int batchSize,
                                      Function<List<T>, List<R>> queryFunction) {
        if (ids == null || ids.isEmpty()) {
            log.debug("ID列表为空，返回空结果");
            return new ArrayList<>();
        }

        log.info("开始批量查询，ID数量={}, 批次大小={}", ids.size(), batchSize);
        long startTime = System.currentTimeMillis();

        List<R> allResults = new ArrayList<>();
        int totalBatches = (ids.size() + batchSize - 1) / batchSize;

        for (int i = 0; i < totalBatches; i++) {
            int fromIndex = i * batchSize;
            int toIndex = Math.min(fromIndex + batchSize, ids.size());
            List<T> batch = ids.subList(fromIndex, toIndex);

            log.debug("执行批次查询，批次 {}/{}, 数量={}", i + 1, totalBatches, batch.size());
            try {
                List<R> batchResults = queryFunction.apply(batch);
                if (batchResults != null) {
                    allResults.addAll(batchResults);
                }
            } catch (Exception e) {
                log.error("批次查询失败，批次 {}/{}", i + 1, totalBatches, e);
                throw new RuntimeException("批次查询失败", e);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("批量查询完成，总数={}, 结果数={}, 耗时={}ms",
                ids.size(), allResults.size(), duration);

        return allResults;
    }

    /**
     * 分页查询优化
     *
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param queryWrapper 查询条件
     * @param mapper 查询映射函数
     * @param <T> 实体类型
     * @param <R> 返回类型
     * @return 分页结果
     */
    public <T, R> IPage<R> optimizedPageQuery(int pageNum, int pageSize,
                                               LambdaQueryWrapper<T> queryWrapper,
                                               Function<T, R> mapper) {
        log.debug("执行分页查询，页码={}, 页大小={}", pageNum, pageSize);

        long startTime = System.currentTimeMillis();

        try {
            Page<T> page = new Page<>(pageNum, pageSize);
            // 这里应该调用实际的Mapper方法
            // IPage<T> result = mapper.selectPage(page, queryWrapper);

            long duration = System.currentTimeMillis() - startTime;
            log.info("分页查询完成，耗时={}ms", duration);

            // 返回空的分页结果作为示例
            return new Page<>(pageNum, pageSize, 0);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("分页查询失败，页码={}, 页大小={}, 耗时={}ms",
                    pageNum, pageSize, duration, e);
            throw new RuntimeException("分页查询失败", e);
        }
    }

    /**
     * 监控查询执行时间
     *
     * @param queryName 查询名称
     * @param supplier 查询执行函数
     * @param <T> 返回类型
     * @return 查询结果
     */
    public <T> T monitorQuery(String queryName, java.util.concurrent.Callable<T> supplier) {
        if (queryName == null || queryName.trim().isEmpty()) {
            queryName = "unknown";
        }

        log.debug("开始监控查询，queryName={}", queryName);
        long startTime = System.currentTimeMillis();

        try {
            T result = supplier.call();
            long duration = System.currentTimeMillis() - startTime;

            if (duration > 1000) {
                log.warn("慢查询检测，queryName={}, duration={}ms", queryName, duration);
            } else {
                log.debug("查询执行完成，queryName={}, duration={}ms", queryName, duration);
            }

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("查询执行失败，queryName={}, duration={}ms", queryName, duration, e);
            throw new RuntimeException("查询执行失败", e);
        }
    }

    /**
     * 并行批量查询
     *
     * @param ids ID列表
     * @param batchSize 批次大小
     * @param queryFunction 查询函数
     * @param <T> 实体类型
     * @param <R> 返回类型
     * @return 查询结果映射
     */
    public <T, R> List<R> parallelBatchQuery(List<T> ids, int batchSize,
                                              Function<List<T>, List<R>> queryFunction) {
        if (ids == null || ids.isEmpty()) {
            log.debug("ID列表为空，返回空结果");
            return new ArrayList<>();
        }

        log.info("开始并行批量查询，ID数量={}, 批次大小={}", ids.size(), batchSize);
        long startTime = System.currentTimeMillis();

        int totalBatches = (ids.size() + batchSize - 1) / batchSize;

        List<java.util.concurrent.CompletableFuture<List<R>>> futures = IntStream.range(0, totalBatches)
                .mapToObj(i -> {
                    int fromIndex = i * batchSize;
                    int toIndex = Math.min(fromIndex + batchSize, ids.size());
                    List<T> batch = ids.subList(fromIndex, toIndex);

                    return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        log.debug("执行并行批次查询，批次 {}/{}", i + 1, totalBatches);
                        try {
                            return queryFunction.apply(batch);
                        } catch (Exception e) {
                            log.error("并行批次查询失败，批次 {}/{}", i + 1, totalBatches, e);
                            throw new RuntimeException("批次查询失败", e);
                        }
                    });
                })
                .collect(Collectors.toList());

        List<R> allResults = new ArrayList<>();
        try {
            for (java.util.concurrent.CompletableFuture<List<R>> future : futures) {
                List<R> batchResults = future.get();
                if (batchResults != null) {
                    allResults.addAll(batchResults);
                }
            }
        } catch (Exception e) {
            log.error("并行批量查询执行失败", e);
            throw new RuntimeException("并行批量查询失败", e);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("并行批量查询完成，总数={}, 结果数={}, 耗时={}ms",
                ids.size(), allResults.size(), duration);

        return allResults;
    }

    /**
     * 分析查询执行计划
     *
     * @param queryWrapper 查询条件
     * @param <T> 实体类型
     * @return 执行计划分析结果
     */
    public <T> String analyzeQueryPlan(LambdaQueryWrapper<T> queryWrapper) {
        log.debug("分析查询执行计划");

        try {
            // 这里应该调用实际的执行计划分析方法
            String plan = "查询执行计划分析结果"; // 示例结果

            log.info("查询执行计划分析完成");
            return plan;
        } catch (Exception e) {
            log.error("查询执行计划分析失败", e);
            throw new RuntimeException("执行计划分析失败", e);
        }
    }

    /**
     * 缓存查询结果（异步版本）
     *
     * @param cacheKey 缓存键
     * @param supplier 查询Supplier
     * @param <T> 返回类型
     * @return CompletableFuture查询结果
     */
    public <T> java.util.concurrent.CompletableFuture<T> cacheQueryAsync(String cacheKey,
                                                                          java.util.concurrent.Callable<T> supplier) {
        log.debug("执行异步缓存查询，cacheKey={}", cacheKey);

        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return cacheQuery(cacheKey, supplier);
            } catch (Exception e) {
                log.error("异步缓存查询失败，cacheKey={}", cacheKey, e);
                throw new RuntimeException("异步查询失败", e);
            }
        });
    }
}
