package com.basebackend.cache.warming;

import com.basebackend.cache.config.CacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 缓存预热管理器
 * 负责管理和调度所有预热任务
 * 
 * 功能：
 * 1. 注册预热任务
 * 2. 按优先级调度任务
 * 3. 支持同步/异步执行
 * 4. 提供进度报告
 * 5. 集成到应用启动流程
 */
@Slf4j
@Component
public class CacheWarmingManager {

    private final CacheProperties cacheProperties;
    private final CacheWarmingExecutor warmingExecutor;
    
    /**
     * 注册的预热任务列表
     */
    private final List<CacheWarmingTask> tasks = new CopyOnWriteArrayList<>();
    
    /**
     * 异步执行线程池
     */
    private ThreadPoolTaskExecutor taskExecutor;
    
    /**
     * 预热进度
     */
    private WarmingProgress progress;

    public CacheWarmingManager(
            CacheProperties cacheProperties,
            CacheWarmingExecutor warmingExecutor) {
        this.cacheProperties = cacheProperties;
        this.warmingExecutor = warmingExecutor;
        initializeThreadPool();
    }

    /**
     * 初始化线程池
     */
    private void initializeThreadPool() {
        taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(2);
        taskExecutor.setMaxPoolSize(5);
        taskExecutor.setQueueCapacity(100);
        taskExecutor.setThreadNamePrefix("cache-warming-");
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        taskExecutor.initialize();
        log.info("Cache warming thread pool initialized");
    }

    /**
     * 注册预热任务
     * 
     * @param task 预热任务
     */
    public void registerWarmingTask(CacheWarmingTask task) {
        if (task == null) {
            log.warn("Cannot register null warming task");
            return;
        }

        if (task.getName() == null || task.getName().trim().isEmpty()) {
            log.warn("Cannot register warming task with empty name");
            return;
        }

        if (task.getDataLoader() == null) {
            log.warn("Cannot register warming task without data loader: {}", task.getName());
            return;
        }

        // 检查是否已存在同名任务
        boolean exists = tasks.stream()
                .anyMatch(t -> t.getName().equals(task.getName()));
        
        if (exists) {
            log.warn("Warming task already registered: {}", task.getName());
            return;
        }

        tasks.add(task);
        log.info("Registered cache warming task: {} (priority: {}, async: {})",
                task.getName(), task.getPriority(), task.isAsync());
    }

    /**
     * 注册多个预热任务
     * 
     * @param tasks 预热任务列表
     */
    public void registerWarmingTasks(List<CacheWarmingTask> tasks) {
        if (tasks != null && !tasks.isEmpty()) {
            tasks.forEach(this::registerWarmingTask);
        }
    }

    /**
     * 应用启动完成后自动执行预热任务
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!cacheProperties.getWarming().isEnabled()) {
            log.info("Cache warming is disabled, skipping");
            return;
        }

        if (tasks.isEmpty()) {
            log.info("No cache warming tasks registered, skipping");
            return;
        }

        log.info("Application ready, starting cache warming with {} tasks", tasks.size());
        
        // 根据配置决定同步或异步执行
        if (cacheProperties.getWarming().isAsync()) {
            executeWarmingTasksAsync();
        } else {
            executeWarmingTasks();
        }
    }

    /**
     * 执行所有预热任务（同步）
     * 按优先级顺序执行
     */
    public void executeWarmingTasks() {
        if (tasks.isEmpty()) {
            log.info("No warming tasks to execute");
            return;
        }

        log.info("Starting synchronous cache warming with {} tasks", tasks.size());
        
        // 初始化进度
        initializeProgress();

        // 按优先级排序（数字越小优先级越高）
        List<CacheWarmingTask> sortedTasks = tasks.stream()
                .sorted(Comparator.comparingInt(CacheWarmingTask::getPriority))
                .collect(Collectors.toList());

        // 执行任务
        for (CacheWarmingTask task : sortedTasks) {
            try {
                boolean success = warmingExecutor.execute(task);
                updateProgress(task, success);
            } catch (Exception e) {
                log.error("Error executing warming task: {}", task.getName(), e);
                task.setStatus(CacheWarmingTask.TaskStatus.FAILED);
                task.setErrorMessage(e.getMessage());
                updateProgress(task, false);
            }
        }

        // 完成进度
        finalizeProgress();
        
        log.info("Cache warming completed: {} tasks, {} success, {} failed, total time: {}ms",
                progress.getTotalTasks(), progress.getSuccessTasks(), 
                progress.getFailedTasks(), progress.getTotalExecutionTime());
    }

    /**
     * 执行所有预热任务（异步）
     * 按优先级分组并发执行
     */
    public void executeWarmingTasksAsync() {
        if (tasks.isEmpty()) {
            log.info("No warming tasks to execute");
            return;
        }

        log.info("Starting asynchronous cache warming with {} tasks", tasks.size());
        
        // 初始化进度
        initializeProgress();

        // 按优先级排序
        List<CacheWarmingTask> sortedTasks = tasks.stream()
                .sorted(Comparator.comparingInt(CacheWarmingTask::getPriority))
                .collect(Collectors.toList());

        // 提交异步任务
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                sortedTasks.stream()
                        .map(task -> CompletableFuture.runAsync(() -> {
                            try {
                                boolean success = warmingExecutor.execute(task);
                                updateProgress(task, success);
                            } catch (Exception e) {
                                log.error("Error executing warming task: {}", task.getName(), e);
                                task.setStatus(CacheWarmingTask.TaskStatus.FAILED);
                                task.setErrorMessage(e.getMessage());
                                updateProgress(task, false);
                            }
                        }, taskExecutor))
                        .toArray(CompletableFuture[]::new)
        );

        // 等待所有任务完成或超时
        try {
            long timeoutMillis = cacheProperties.getWarming().getTimeout().toMillis();
            allTasks.get(timeoutMillis, TimeUnit.MILLISECONDS);
            log.info("All async warming tasks completed");
        } catch (TimeoutException e) {
            log.warn("Cache warming timeout after {}ms, some tasks may not complete",
                    cacheProperties.getWarming().getTimeout().toMillis());
        } catch (Exception e) {
            log.error("Error waiting for warming tasks to complete", e);
        }

        // 完成进度
        finalizeProgress();
        
        log.info("Async cache warming completed: {} tasks, {} success, {} failed, total time: {}ms",
                progress.getTotalTasks(), progress.getSuccessTasks(), 
                progress.getFailedTasks(), progress.getTotalExecutionTime());
    }

    /**
     * 初始化进度
     */
    private void initializeProgress() {
        progress = WarmingProgress.builder()
                .totalTasks(tasks.size())
                .completedTasks(0)
                .successTasks(0)
                .failedTasks(0)
                .totalItems(0)
                .loadedItems(0)
                .failedItems(0)
                .startTime(System.currentTimeMillis())
                .tasks(new ArrayList<>(tasks))
                .build();
    }

    /**
     * 更新进度
     */
    private synchronized void updateProgress(CacheWarmingTask task, boolean success) {
        progress.setCompletedTasks(progress.getCompletedTasks() + 1);
        
        if (success) {
            if (task.getStatus() == CacheWarmingTask.TaskStatus.SUCCESS) {
                progress.setSuccessTasks(progress.getSuccessTasks() + 1);
            } else if (task.getStatus() == CacheWarmingTask.TaskStatus.PARTIAL_SUCCESS) {
                progress.setSuccessTasks(progress.getSuccessTasks() + 1);
            }
        } else {
            progress.setFailedTasks(progress.getFailedTasks() + 1);
        }

        progress.setTotalItems(progress.getTotalItems() + task.getItemCount());
        progress.setLoadedItems(progress.getLoadedItems() + task.getLoadedCount());
        progress.setFailedItems(progress.getFailedItems() + task.getFailedCount());

        log.debug("Warming progress: {}/{} tasks completed ({:.1f}%)",
                progress.getCompletedTasks(), progress.getTotalTasks(), 
                progress.getCompletionPercentage());
    }

    /**
     * 完成进度
     */
    private void finalizeProgress() {
        progress.setEndTime(System.currentTimeMillis());
    }

    /**
     * 获取预热进度
     * 
     * @return 预热进度
     */
    public WarmingProgress getProgress() {
        return progress;
    }

    /**
     * 获取所有注册的任务
     * 
     * @return 任务列表
     */
    public List<CacheWarmingTask> getTasks() {
        return new ArrayList<>(tasks);
    }

    /**
     * 清除所有任务
     */
    public void clearTasks() {
        tasks.clear();
        log.info("All warming tasks cleared");
    }

    /**
     * 获取任务数量
     * 
     * @return 任务数量
     */
    public int getTaskCount() {
        return tasks.size();
    }

    /**
     * 清理资源
     */
    @PreDestroy
    public void destroy() {
        log.info("Shutting down cache warming manager");
        if (taskExecutor != null) {
            taskExecutor.shutdown();
            try {
                if (!taskExecutor.getThreadPoolExecutor().awaitTermination(10, TimeUnit.SECONDS)) {
                    taskExecutor.getThreadPoolExecutor().shutdownNow();
                }
            } catch (InterruptedException e) {
                taskExecutor.getThreadPoolExecutor().shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("Cache warming manager shut down");
    }
}
