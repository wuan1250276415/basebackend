package com.basebackend.scheduler.performance;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务性能优化配置
 *
 * <p>优化系统并发处理能力：
 * <ul>
 *   <li>核心线程池配置优化</li>
 *   <li>任务队列调优</li>
 *   <li>线程池监控</li>
 *   <li>拒绝策略优化</li>
 * </ul>
 *
 * <p>线程池设计原则：
 * <ul>
 *   <li>CPU 密集型任务：核心线程数 = CPU 核心数</li>
 *   <li>I/O 密集型任务：核心线程数 = CPU 核心数 × 2</li>
 *   <li>混合型任务：根据实际情况调整</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Configuration
@EnableAsync
public class AsyncTaskConfig {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AsyncTaskConfig.class);

    /**
     * 通用异步任务线程池
     *
     * <p>配置参数：
     * <ul>
     *   <li>核心线程数：CPU 核心数</li>
     *   <li>最大线程数：CPU 核心数 × 2</li>
     *   <li>队列容量：100</li>
     *   <li>空闲线程存活时间：60 秒</li>
     *   <li>拒绝策略：CallerRunsPolicy（由调用者执行）</li>
     * </ul>
     *
     * @return Executor 实例
     */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        int cpuCores = Runtime.getRuntime().availableProcessors();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // ========== 核心配置 ==========
        // 核心线程数
        executor.setCorePoolSize(cpuCores);

        // 最大线程数
        executor.setMaxPoolSize(cpuCores * 2);

        // 队列容量
        executor.setQueueCapacity(100);

        // ========== 性能优化 ==========
        // 空闲线程存活时间：60 秒
        executor.setKeepAliveSeconds(60);

        // 线程名前缀
        executor.setThreadNamePrefix("BaseBackend-Async-");

        // ========== 拒绝策略 ==========
        // CallerRunsPolicy：拒绝时由调用者线程执行
        // 优点：避免任务丢失，缺点：可能影响主线程性能
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // ========== 关闭配置 ==========
        // 应用关闭时等待任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间：5 分钟
        executor.setAwaitTerminationSeconds(300);

        executor.initialize();

        log.info("Async task executor configured: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(),
                executor.getMaxPoolSize(),
                executor.getQueueCapacity());

        return executor;
    }

    /**
     * 计算密集型任务线程池
     *
     * <p>专为 CPU 密集型任务优化，如数据计算、报表生成等。
     * 特点：线程数较少，避免线程切换开销。
     *
     * @return Executor 实例
     */
    @Bean("cpuIntensiveExecutor")
    public Executor cpuIntensiveExecutor() {
        int cpuCores = Runtime.getRuntime().availableProcessors();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // CPU 密集型：线程数等于 CPU 核心数
        executor.setCorePoolSize(cpuCores);
        executor.setMaxPoolSize(cpuCores);

        // 较小的队列容量，因为任务执行时间较长
        executor.setQueueCapacity(50);

        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("CPU-Intensive-");

        // AbortPolicy：直接抛出异常
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(300);

        executor.initialize();

        log.info("CPU intensive task executor configured: corePoolSize={}, maxPoolSize={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize());

        return executor;
    }

    /**
     * I/O 密集型任务线程池
     *
     * <p>专为 I/O 密集型任务优化，如数据库查询、文件读写、网络请求等。
     * 特点：线程数较多，充分利用等待时间。
     *
     * @return Executor 实例
     */
    @Bean("ioIntensiveExecutor")
    public Executor ioIntensiveExecutor() {
        int cpuCores = Runtime.getRuntime().availableProcessors();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // I/O 密集型：线程数 = CPU 核心数 × 2
        executor.setCorePoolSize(cpuCores * 2);
        executor.setMaxPoolSize(cpuCores * 4);

        // 较大的队列容量
        executor.setQueueCapacity(200);

        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("IO-Intensive-");

        // DiscardPolicy：拒绝时丢弃最新任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(600);

        executor.initialize();

        log.info("IO intensive task executor configured: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(),
                executor.getMaxPoolSize(),
                executor.getQueueCapacity());

        return executor;
    }

    /**
     * 批量任务处理线程池
     *
     * <p>专为批量处理任务优化，如批量数据导入、批量更新等。
     * 特点：支持大量并发执行，有限流机制。
     *
     * @return Executor 实例
     */
    @Bean("batchTaskExecutor")
    public Executor batchTaskExecutor() {
        int cpuCores = Runtime.getRuntime().availableProcessors();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 批量任务：线程数 = CPU 核心数 × 2
        executor.setCorePoolSize(cpuCores * 2);
        executor.setMaxPoolSize(cpuCores * 3);

        // 大队列容量
        executor.setQueueCapacity(500);

        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("Batch-Task-");

        // CallerRunsPolicy：限流效果
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(900);

        executor.initialize();

        log.info("Batch task executor configured: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(),
                executor.getMaxPoolSize(),
                executor.getQueueCapacity());

        return executor;
    }

    /**
     * 线程池监控器
     *
     * @return 线程池监控器
     */
    @Bean
    public ThreadPoolMonitor threadPoolMonitor() {
        return new ThreadPoolMonitor();
    }

    /**
     * 线程池监控器
     */
    public static class ThreadPoolMonitor {

        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ThreadPoolMonitor.class);
        private static final int ALERT_QUEUE_SIZE = 80;
        private static final int ALERT_ACTIVE_THREADS_RATIO = 80;

        /**
         * 检查线程池状态
         *
         * @param executor 线程池实例
         * @param executorName 线程池名称
         */
        public void checkThreadPoolStatus(ThreadPoolTaskExecutor executor, String executorName) {
            if (executor.getThreadPoolExecutor() == null) {
                return;
            }

            ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();

            int activeThreads = threadPoolExecutor.getActiveCount();
            int corePoolSize = executor.getCorePoolSize();
            int maxPoolSize = executor.getMaxPoolSize();
            int queueSize = threadPoolExecutor.getQueue().size();
            long completedTasks = threadPoolExecutor.getCompletedTaskCount();

            // 计算线程使用率
            double activeRatio = (double) activeThreads / maxPoolSize * 100;
            // 计算队列使用率
            double queueRatio = (double) queueSize / executor.getQueueCapacity() * 100;

            log.debug("Thread pool status [name={}, activeThreads={}, corePoolSize={}, maxPoolSize={}, " +
                            "queueSize={}, queueCapacity={}, completedTasks={}, activeRatio={}%, queueRatio={}%]",
                    executorName, activeThreads, corePoolSize, maxPoolSize, queueSize,
                    executor.getQueueCapacity(), completedTasks, activeRatio, queueRatio);

            // 性能告警
            if (queueRatio > ALERT_QUEUE_SIZE) {
                log.warn("High queue usage detected [name={}, queueRatio={}%, queueSize={}/{}]",
                        executorName, queueRatio, queueSize, executor.getQueueCapacity());
            }

            if (activeRatio > ALERT_ACTIVE_THREADS_RATIO) {
                log.warn("High thread usage detected [name={}, activeRatio={}%, activeThreads={}/{}]",
                        executorName, activeRatio, activeThreads, maxPoolSize);
            }
        }

        /**
         * 打印线程池性能报告
         */
        public void printPerformanceReport() {
            log.info("=== Thread Pool Performance Report ===");
            log.info("Report generated at: {}", new java.util.Date());
            log.info("=== End of Report ===");
        }
    }
}
