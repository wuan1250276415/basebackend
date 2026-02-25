package com.basebackend.cache.refresh;

import com.basebackend.cache.config.CacheProperties;
import com.basebackend.cache.lock.DistributedLockService;
import com.basebackend.cache.service.RedisService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 近过期刷新自动配置
 * 创建刷新线程池和 NearExpiryRefreshManager Bean
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "basebackend.cache.refresh", name = "enabled", havingValue = "true")
public class NearExpiryRefreshAutoConfiguration {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService cacheRefreshExecutor(CacheProperties cacheProperties) {
        int poolSize = cacheProperties.getRefresh().getPoolSize();
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
                poolSize,
                new DaemonThreadFactory("cache-refresh"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        log.info("Cache refresh thread pool created: poolSize={}", poolSize);
        return executor;
    }

    @Bean
    public NearExpiryRefreshManager nearExpiryRefreshManager(
            RedisService redisService,
            DistributedLockService distributedLockService,
            CacheProperties cacheProperties,
            ExecutorService cacheRefreshExecutor,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        log.info("Registering NearExpiryRefreshManager");
        return new NearExpiryRefreshManager(
                redisService, distributedLockService, cacheProperties,
                cacheRefreshExecutor, meterRegistry);
    }

    /**
     * 守护线程工厂
     */
    private static class DaemonThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(1);
        private final String prefix;

        DaemonThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, prefix + "-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
