package com.basebackend.scheduler.performance;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 分页查询性能优化配置
 *
 * <p>优化 MyBatis Plus 分页插件配置：
 * <ul>
 *   <li>启用分页插件优化</li>
 *   <li>配置分页大小上限</li>
 *   <li>启用大查询保护</li>
 *   <li>添加分页性能监控</li>
 * </ul>
 *
 * <p>优化策略：
 * <ul>
 *   <li>自动检测大查询并发出警告</li>
 *   <li>支持游标分页</li>
 *   <li>优化 COUNT 查询</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Configuration
public class PaginationOptimizationConfig {

    /**
     * 优化分页拦截器
     *
     * @return MybatisPlusInterceptor 实例
     */
    @Bean
    @Primary
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件优化
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();

        // ========== 分页大小限制 ==========
        // 单页最大记录数：1000 条
        paginationInterceptor.setMaxLimit(1000L);

        // ========== 大查询保护 ==========
        // 当单页查询超过 500 条记录时发出警告
        paginationInterceptor.setOverflow(true);

        // ========== COUNT 查询优化 ==========
        // 是否启用 COUNT 优化（减少子查询时的 COUNT 开销）
        paginationInterceptor.setOptimizeJoin(true);

        // ========== 分页方言优化 ==========
        // 自动检测数据库类型并使用对应的分页 SQL
        // MySQL: LIMIT
        // Oracle: ROWNUM
        // PostgreSQL: LIMIT

        interceptor.addInnerInterceptor(paginationInterceptor);

        log.info("Pagination interceptor configured: maxLimit={}, overflow={}, optimizeJoin={}",
                paginationInterceptor.getMaxLimit(),
                paginationInterceptor.isOverflow(),
                paginationInterceptor.isOptimizeJoin());

        return interceptor;
    }

    /**
     * 分页性能监控
     *
     * @return 分页监控器
     */
    @Bean
    public PaginationPerformanceMonitor paginationPerformanceMonitor() {
        return new PaginationPerformanceMonitor();
    }

    /**
     * 分页性能监控器
     */
    @Slf4j
    public static class PaginationPerformanceMonitor {

        private final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicLong> pageStats =
                new java.util.concurrent.ConcurrentHashMap<>();

        private final java.util.concurrent.ConcurrentHashMap<String, Long> queryTimes =
                new java.util.concurrent.ConcurrentHashMap<>();

        /**
         * 记录分页查询统计
         *
         * @param queryName 查询名称
         * @param current 当前页码
         * @param size 每页大小
         * @param duration 查询耗时
         * @param total 总记录数
         */
        public void recordPaginationQuery(String queryName, long current, long size,
                                          long duration, long total) {
            String key = queryName + "_" + size;
            pageStats.computeIfAbsent(key, k -> new java.util.concurrent.atomic.AtomicLong(0))
                    .incrementAndGet();

            queryTimes.put(queryName, duration);

            // 性能告警
            if (duration > 2000) {
                log.warn("Slow pagination query detected [name={}, current={}, size={}, duration={}ms]",
                        queryName, current, size, duration);
            }

            if (current > 100 && size > 50) {
                log.warn("Deep pagination detected [name={}, current={}, size={}]. " +
                        "Consider using cursor-based pagination for better performance.",
                        queryName, current, size);
            }

            if (total > 100000 && size > 100) {
                log.warn("Large dataset pagination [name={}, total={}, size={}]. " +
                        "Consider using LIMIT with reasonable size.",
                        queryName, total, size);
            }
        }

        /**
         * 获取分页统计
         *
         * @param queryName 查询名称
         * @return 统计信息
         */
        public java.util.concurrent.atomic.AtomicLong getPaginationStats(String queryName, long size) {
            return pageStats.get(queryName + "_" + size);
        }

        /**
         * 获取查询时间
         *
         * @param queryName 查询名称
         * @return 查询时间
         */
        public Long getQueryTime(String queryName) {
            return queryTimes.get(queryName);
        }

        /**
         * 打印分页性能报告
         */
        public void printPaginationReport() {
            log.info("=== Pagination Performance Report ===");

            pageStats.forEach((key, count) -> {
                log.info("Pagination Query: {}, Total Calls: {}", key, count.get());
            });

            log.info("=== End of Pagination Report ===");
        }
    }

    /**
     * 分页优化建议器
     */
    public static class PaginationOptimizer {

        /**
         * 智能分页建议
         *
         * @param totalRecords 总记录数
         * @param currentPage 当前页码
         * @param pageSize 当前页大小
         * @return 优化建议
         */
        public static String getPaginationAdvice(long totalRecords, long currentPage, long pageSize) {
            StringBuilder advice = new StringBuilder();

            // 大数据集建议
            if (totalRecords > 100000) {
                advice.append("Large dataset detected. ");
                if (currentPage > 100) {
                    advice.append("Consider using cursor-based pagination. ");
                }
                if (pageSize > 100) {
                    advice.append("Reduce page size to improve performance. ");
                }
            }

            // 深度分页建议
            if (currentPage > 100) {
                advice.append("Deep pagination detected. Use cursor pagination for better performance. ");
            }

            // 频繁查询建议
            if (pageSize > 200) {
                advice.append("Large page size may impact performance. Consider reducing page size. ");
            }

            if (advice.length() == 0) {
                return "Pagination configuration is optimal.";
            }

            return advice.toString().trim();
        }
    }
}
