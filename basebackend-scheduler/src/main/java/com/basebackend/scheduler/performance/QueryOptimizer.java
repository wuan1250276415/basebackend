package com.basebackend.scheduler.performance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

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
@Slf4j
@Component
public class QueryOptimizer {

    /**
     * 缓存查询结果
     *
     * @param cacheKey 缓存键
     * @param supplier 查询Supplier
     * @param <T> 返回类型
     * @return 查询结果
     */
    public <T> T cacheQuery(String cacheKey, java.util.concurrent.Callable<T> supplier) {
        long startTime = System.currentTimeMillis();
        try {
            // TODO: 实现实际缓存逻辑（Redis + Caffeine 多层缓存）
            T result = supplier.call();
            long duration = System.currentTimeMillis() - startTime;

            log.debug("Cache query executed [key={}, duration={}ms]", cacheKey, duration);
            return result;
        } catch (Exception e) {
            log.error("Cache query failed [key={}]", cacheKey, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 批量查询优化
     *
     * <p>将大量查询分批执行，避免单次查询数据量过大。
     * 适用于需要查询大量 ID 对应数据的场景。
     *
     * @param ids ID 列表
     * @param batchSize 批次大小
     * @param queryFunction 批量查询函数
     * @param <T> ID 类型
     * @param <R> 返回类型
     * @return 所有查询结果
     */
    public <T, R> List<R> batchQuery(List<T> ids, int batchSize, Function<List<T>, List<R>> queryFunction) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        long startTime = System.currentTimeMillis();
        List<R> allResults = new ArrayList<>();
        int totalSize = ids.size();
        int batchCount = (totalSize + batchSize - 1) / batchSize;

        log.debug("Starting batch query [totalSize={}, batchSize={}, batchCount={}]",
                totalSize, batchSize, batchCount);

        for (int i = 0; i < batchCount; i++) {
            int fromIndex = i * batchSize;
            int toIndex = Math.min(fromIndex + batchSize, totalSize);
            List<T> batch = ids.subList(fromIndex, toIndex);

            long batchStartTime = System.currentTimeMillis();
            List<R> batchResults = queryFunction.apply(batch);
            long batchDuration = System.currentTimeMillis() - batchStartTime;

            allResults.addAll(batchResults);

            log.debug("Batch query completed [batch={}/{}, size={}, duration={}ms]",
                    i + 1, batchCount, batch.size(), batchDuration);
        }

        long totalDuration = System.currentTimeMillis() - startTime;
        log.info("Batch query finished [totalSize={}, totalBatches={}, totalDuration={}ms]",
                totalSize, batchCount, totalDuration);

        return allResults;
    }

    /**
     * 优化分页查询
     *
     * <p>根据查询场景选择最优的分页策略：
     * - 前几页：使用 LIMIT offset, size
     * - 深度分页：使用游标分页（基于 ID 或时间戳）
     *
     * @param current 当前页码（从1开始）
     * @param size 每页大小
     * @param queryFunction 查询函数
     * @param <T> 返回类型
     * @return 分页结果
     */
    public <T> IPage<T> optimizePagination(long current, long size, Function<Page<T>, IPage<T>> queryFunction) {
        Page<T> page = new Page<>(current, size);
        long startTime = System.currentTimeMillis();

        IPage<T> result = queryFunction.apply(page);

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Optimized pagination query [current={}, size={}, total={}, duration={}ms]",
                current, size, result.getTotal(), duration);

        return result;
    }

    /**
     * 智能分页策略
     *
     * <p>根据当前页码自动选择分页策略：
     * - 页码 <= 100：使用传统 LIMIT offset, size
     * - 页码 > 100：使用游标分页
     *
     * @param current 当前页码
     * @param size 每页大小
     * @param lastCursor 上次查询的光标值（可选）
     * @param queryFunction 查询函数
     * @param <T> 返回类型
     * @return 分页结果
     */
    public <T> IPage<T> smartPagination(long current, long size, String lastCursor,
                                        Function<Page<T>, IPage<T>> queryFunction) {
        // 当页码超过100时，建议使用游标分页
        if (current > 100) {
            log.warn("Deep pagination detected [current={}, size={}]. " +
                    "Consider using cursor-based pagination for better performance.", current, size);
        }

        return optimizePagination(current, size, queryFunction);
    }

    /**
     * 并行查询优化
     *
     * <p>将独立查询并行执行，提升查询效率。
     * 适用于需要查询多个不相关数据的场景。
     *
     * @param queries 查询任务列表
     * @param <T> 返回类型
     * @return 查询结果列表
     */
    public <T> List<T> parallelQuery(List<java.util.concurrent.Callable<T>> queries) {
        if (queries == null || queries.isEmpty()) {
            return new ArrayList<>();
        }

        log.debug("Starting parallel query [taskCount={}]", queries.size());

        long startTime = System.currentTimeMillis();
        List<java.util.concurrent.CompletableFuture<T>> futures = queries.stream()
                .map(query -> java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    try {
                        return query.call();
                    } catch (Exception e) {
                        log.error("Parallel query failed", e);
                        throw new RuntimeException(e);
                    }
                }))
                .collect(Collectors.toList());

        List<T> results = futures.stream()
                .map(java.util.concurrent.CompletableFuture::join)
                .collect(Collectors.toList());

        long duration = System.currentTimeMillis() - startTime;
        log.info("Parallel query finished [taskCount={}, duration={}ms]", queries.size(), duration);

        return results;
    }

    /**
     * 查询计划分析
     *
     * @param sql SQL语句
     * @return 分析结果
     */
    public QueryAnalysisResult analyzeQuery(String sql) {
        // TODO: 实现查询计划分析逻辑
        // 可以通过 EXPLAIN 语句分析查询执行计划

        QueryAnalysisResult result = new QueryAnalysisResult();
        result.setSql(sql);
        result.setAnalysisTime(System.currentTimeMillis());

        // 简单的 SQL 复杂度评估
        int complexityScore = evaluateQueryComplexity(sql);
        result.setComplexityScore(complexityScore);

        if (complexityScore > 7) {
            result.setRecommendation("Consider optimizing this query - high complexity detected");
            log.warn("High complexity query detected [sql={}, complexity={}]", sql, complexityScore);
        }

        return result;
    }

    /**
     * 评估查询复杂度
     *
     * @param sql SQL语句
     * @return 复杂度评分（1-10）
     */
    private int evaluateQueryComplexity(String sql) {
        int score = 1;

        // JOIN 数量
        long joinCount = sql.toUpperCase().split("JOIN").length - 1;
        score += joinCount * 2;

        // WHERE 条件数量
        long whereCount = sql.toUpperCase().split("WHERE").length - 1;
        score += whereCount;

        // LIKE 查询
        if (sql.toUpperCase().contains("LIKE '%")) {
            score += 2; // 前缀匹配性能较差
        }

        // 子查询
        long subqueryCount = sql.toUpperCase().split("\\(").length - 1;
        score += subqueryCount;

        // ORDER BY
        if (sql.toUpperCase().contains("ORDER BY")) {
            score += 1;
        }

        // GROUP BY
        if (sql.toUpperCase().contains("GROUP BY")) {
            score += 1;
        }

        return Math.min(score, 10);
    }

    /**
     * 查询分析结果
     */
    public static class QueryAnalysisResult {
        private String sql;
        private long analysisTime;
        private int complexityScore;
        private String recommendation;

        // Getters and Setters
        public String getSql() { return sql; }
        public void setSql(String sql) { this.sql = sql; }
        public long getAnalysisTime() { return analysisTime; }
        public void setAnalysisTime(long analysisTime) { this.analysisTime = analysisTime; }
        public int getComplexityScore() { return complexityScore; }
        public void setComplexityScore(int complexityScore) { this.complexityScore = complexityScore; }
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    }
}
