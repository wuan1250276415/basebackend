package com.basebackend.cache.warming;

import com.basebackend.cache.manager.MultiLevelCacheManager;
import com.basebackend.cache.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 缓存预热执行器
 * 负责执行单个预热任务
 */
@Slf4j
@Component
public class CacheWarmingExecutor {

    private final RedisService redisService;
    private final MultiLevelCacheManager multiLevelCacheManager;

    public CacheWarmingExecutor(
            RedisService redisService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) 
            MultiLevelCacheManager multiLevelCacheManager) {
        this.redisService = redisService;
        this.multiLevelCacheManager = multiLevelCacheManager;
    }

    /**
     * 执行预热任务
     * 
     * @param task 预热任务
     * @return 是否执行成功
     */
    public boolean execute(CacheWarmingTask task) {
        if (task == null) {
            log.warn("Cache warming task is null, skipping");
            return false;
        }

        log.info("Starting cache warming task: {} (priority: {})", task.getName(), task.getPriority());
        
        task.setStatus(CacheWarmingTask.TaskStatus.RUNNING);
        task.setStartTime(System.currentTimeMillis());

        try {
            // 调用数据加载器获取数据
            Map<String, Object> data = task.getDataLoader().get();
            
            if (data == null || data.isEmpty()) {
                log.warn("No data loaded for warming task: {}", task.getName());
                task.setStatus(CacheWarmingTask.TaskStatus.SUCCESS);
                task.setItemCount(0);
                task.setLoadedCount(0);
                task.setEndTime(System.currentTimeMillis());
                return true;
            }

            task.setItemCount(data.size());
            log.info("Loaded {} items for warming task: {}", data.size(), task.getName());

            // 加载数据到缓存
            int loadedCount = 0;
            int failedCount = 0;

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                try {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    Duration ttl = task.getTtl();

                    // 根据是否启用多级缓存选择不同的加载方式
                    if (multiLevelCacheManager != null) {
                        // 使用多级缓存管理器
                        multiLevelCacheManager.set(key, value, ttl);
                        log.debug("Warmed cache (multi-level) for key: {}", key);
                    } else {
                        // 直接使用 Redis
                        if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                            redisService.set(key, value, ttl.getSeconds(), TimeUnit.SECONDS);
                        } else {
                            redisService.set(key, value);
                        }
                        log.debug("Warmed cache (Redis) for key: {}", key);
                    }

                    loadedCount++;
                } catch (Exception e) {
                    failedCount++;
                    log.error("Failed to warm cache for key: {}", entry.getKey(), e);
                }
            }

            task.setLoadedCount(loadedCount);
            task.setFailedCount(failedCount);
            task.setEndTime(System.currentTimeMillis());

            // 判断任务状态
            if (failedCount == 0) {
                task.setStatus(CacheWarmingTask.TaskStatus.SUCCESS);
                log.info("Cache warming task completed successfully: {} ({} items loaded in {}ms)",
                        task.getName(), loadedCount, task.getExecutionTime());
                return true;
            } else if (loadedCount > 0) {
                task.setStatus(CacheWarmingTask.TaskStatus.PARTIAL_SUCCESS);
                log.warn("Cache warming task partially completed: {} ({} items loaded, {} failed in {}ms)",
                        task.getName(), loadedCount, failedCount, task.getExecutionTime());
                return true;
            } else {
                task.setStatus(CacheWarmingTask.TaskStatus.FAILED);
                task.setErrorMessage("All items failed to load");
                log.error("Cache warming task failed: {} (all {} items failed)", task.getName(), failedCount);
                return false;
            }

        } catch (Exception e) {
            task.setStatus(CacheWarmingTask.TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            task.setEndTime(System.currentTimeMillis());
            log.error("Cache warming task failed with exception: {}", task.getName(), e);
            return false;
        }
    }
}
