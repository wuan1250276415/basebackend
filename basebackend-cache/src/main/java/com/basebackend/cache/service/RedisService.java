package com.basebackend.cache.service;

import com.basebackend.cache.config.CacheProperties;
import com.basebackend.cache.exception.CacheConnectionException;
import com.basebackend.cache.serializer.CacheSerializer;
import com.basebackend.cache.serializer.SerializerFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Redis 缓存服务
 * 增强版本，支持批量操作、模式匹配删除、pipeline、序列化器集成和容错降级
 */
@Slf4j
@Service
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheProperties cacheProperties;
    @Getter
    private final CacheSerializer serializer;
    
    // 熔断器状态
    private volatile CircuitBreakerState circuitBreakerState = CircuitBreakerState.CLOSED;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong circuitOpenTime = new AtomicLong(0);
    private final AtomicInteger halfOpenSuccesses = new AtomicInteger(0);
    
    // 超时执行器
    private final ExecutorService timeoutExecutor;

    public RedisService(RedisTemplate<String, Object> redisTemplate, CacheProperties cacheProperties) {
        this.redisTemplate = redisTemplate;
        this.cacheProperties = cacheProperties;
        this.serializer = SerializerFactory.getSerializer(cacheProperties.getSerialization().getType());
        this.timeoutExecutor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "redis-timeout-executor");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 熔断器状态枚举
     */
    private enum CircuitBreakerState {
        CLOSED,      // 正常状态
        OPEN,        // 熔断打开
        HALF_OPEN    // 半开状态（尝试恢复）
    }

    // ========== 批量操作 ==========

    /**
     * 批量获取缓存
     * 使用 pipeline 提高性能
     *
     * @param keys 键集合
     * @return 键值对映射
     */
    public <T> Map<String, T> multiGet(Set<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        return executeWithFallback(() -> {
            List<Object> values = redisTemplate.opsForValue().multiGet(keys);
            Map<String, T> result = new HashMap<>();
            
            if (values != null) {
                Iterator<String> keyIterator = keys.iterator();
                Iterator<Object> valueIterator = values.iterator();
                
                while (keyIterator.hasNext() && valueIterator.hasNext()) {
                    String key = keyIterator.next();
                    Object value = valueIterator.next();
                    if (value != null) {
                        @SuppressWarnings("unchecked")
                        T typedValue = (T) value;
                        result.put(key, typedValue);
                    }
                }
            }
            
            return result;
        }, Collections.emptyMap(), "multiGet");
    }

    /**
     * 批量设置缓存
     * 使用 pipeline 提高性能
     *
     * @param entries 键值对映射
     * @param ttl 过期时间
     */
    public void multiSet(Map<String, Object> entries, Duration ttl) {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        executeWithFallback(() -> {
            if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                // 使用 pipeline 批量设置带过期时间的缓存
                redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    entries.forEach((key, value) -> {
                        redisTemplate.opsForValue().set(key, value, ttl.getSeconds(), TimeUnit.SECONDS);
                    });
                    return null;
                });
            } else {
                // 不带过期时间的批量设置
                redisTemplate.opsForValue().multiSet(entries);
            }
            return null;
        }, null, "multiSet");
    }

    /**
     * 批量设置缓存（不设置过期时间）
     *
     * @param entries 键值对映射
     */
    public void multiSet(Map<String, Object> entries) {
        multiSet(entries, null);
    }

    /**
     * 根据模式删除键
     * 使用 SCAN 命令游标分页 + UNLINK 非阻塞删除
     * 替代原来的 keys() + 批量删除，避免阻塞 Redis
     *
     * @param pattern 键模式（支持通配符 * 和 ?）
     * @return 删除的键数量
     */
    public long deleteByPattern(String pattern) {
        return deleteByPattern(pattern, 1000);
    }

    /**
     * 根据模式删除键（指定批量大小）
     * 使用 SCAN 命令游标分页 + UNLINK 非阻塞删除
     *
     * @param pattern 键模式（支持通配符 * 和 ?）
     * @param batchSize 批量删除大小，建议500-5000
     * @return 删除的键数量
     */
    public long deleteByPattern(String pattern, int batchSize) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return 0;
        }

        if (batchSize <= 0) {
            batchSize = 1000; // 默认值
        }

        long startTime = System.currentTimeMillis();

        // 创建最终副本以供 lambda 使用
        final String finalPattern = pattern;
        final int finalBatchSize = batchSize;

        try {
            log.info("Starting pattern-based deletion for pattern: {}, batch size: {}", finalPattern, finalBatchSize);

            // 使用 AtomicLong 和 AtomicInteger 来跟踪状态
            final long[] totalDeleted = {0};
            final int[] batchCount = {0};

            // 使用 SCAN 游标分页遍历所有匹配的键
            ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(finalPattern)
                .count(finalBatchSize)
                .build();

            redisTemplate.execute((RedisCallback<Void>) connection -> {
                List<byte[]> currentBatch = new ArrayList<>();

                try (Cursor<byte[]> cursor = connection.scan(scanOptions)) {
                    while (cursor.hasNext()) {
                        currentBatch.add(cursor.next());

                        // 当批次达到指定大小时执行删除
                        if (currentBatch.size() >= finalBatchSize) {
                            batchCount[0]++;
                            totalDeleted[0] += deleteBatch(currentBatch);
                            log.debug("Processed batch #{}: {} keys, total deleted: {}",
                                batchCount[0], currentBatch.size(), totalDeleted[0]);
                            currentBatch.clear();
                        }
                    }

                    // 删除剩余的键
                    if (!currentBatch.isEmpty()) {
                        batchCount[0]++;
                        totalDeleted[0] += deleteBatch(currentBatch);
                        log.debug("Processed final batch #{}: {} keys, total deleted: {}",
                            batchCount[0], currentBatch.size(), totalDeleted[0]);
                    }
                }

                return null;
            });

            long duration = System.currentTimeMillis() - startTime;
            log.info("Pattern-based deletion completed: {} keys deleted in {} ms ({} batches), pattern: {}",
                totalDeleted[0], duration, batchCount[0], finalPattern);

            return totalDeleted[0];

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Pattern-based deletion failed after {} ms, pattern: {}", duration, finalPattern, e);

            // 失败时降级到原来的 keys() 方法
            log.warn("Falling back to keys() + batch delete for pattern: {} (this may block Redis!)", finalPattern);
            return executeWithFallback(() -> {
                Set<String> keys = redisTemplate.keys(finalPattern);
                if (keys != null && !keys.isEmpty()) {
                    Long deleted = redisTemplate.delete(keys);
                    long result = deleted != null ? deleted : 0L;
                    log.warn("FALLBACK: Deleted {} keys using keys() method for pattern: {}", result, finalPattern);
                    return result;
                }
                return 0L;
            }, 0L, "deleteByPattern");
        }
    }

    /**
     * 批量删除键（使用 UNLINK 命令）
     *
     * @param keys 要删除的键列表
     * @return 删除的键数量
     */
    private long deleteBatch(List<byte[]> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }

        try {
            // 使用 UNLINK 命令（异步删除，不会阻塞 Redis）
            Long deleted = redisTemplate.execute((RedisCallback<Long>) connection -> {
                long count = 0;
                for (byte[] key : keys) {
                    Long result = connection.unlink(key);
                    if (result != null && result > 0) {
                        count++;
                    }
                }
                return count;
            });

            return deleted != null ? deleted : 0;
        } catch (Exception e) {
            log.error("Error deleting batch of {} keys", keys.size(), e);
            // 如果 UNLINK 失败，尝试使用 DEL
            try {
                List<String> stringKeys = keys.stream()
                    .map(String::new)
                    .collect(Collectors.toList());
                Long deleted = redisTemplate.delete(stringKeys);
                return deleted != null ? deleted : 0;
            } catch (Exception ex) {
                log.error("Error deleting batch with DEL fallback", ex);
                return 0;
            }
        }
    }

    /**
     * 使用 pipeline 批量执行操作
     * 提高批量操作性能
     *
     * @param operations 操作列表
     * @return 操作结果列表
     */
    public List<Object> executePipeline(List<PipelineOperation> operations) {
        if (operations == null || operations.isEmpty()) {
            return Collections.emptyList();
        }

        return executeWithFallback(() -> {
            return redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (PipelineOperation operation : operations) {
                    operation.execute(connection);
                }
                return null;
            });
        }, Collections.emptyList(), "executePipeline");
    }

    /**
     * Pipeline 操作接口
     */
    @FunctionalInterface
    public interface PipelineOperation {
        void execute(org.springframework.data.redis.connection.RedisConnection connection);
    }

    // ========== 容错和降级 ==========

    /**
     * 执行 Redis 操作，带有容错和降级逻辑
     *
     * @param operation 要执行的操作
     * @param fallbackValue 降级返回值
     * @param operationName 操作名称（用于日志）
     * @return 操作结果或降级值
     */
    private <T> T executeWithFallback(RedisOperation<T> operation, T fallbackValue, String operationName) {
        // 检查熔断器状态
        if (cacheProperties.getResilience().getCircuitBreaker().isEnabled()) {
            CircuitBreakerState currentState = checkCircuitBreakerState();
            
            if (currentState == CircuitBreakerState.OPEN) {
                log.warn("Circuit breaker is OPEN for operation: {}, using fallback", operationName);
                recordCircuitBreakerMetrics(operationName, false);
                return handleFallback(fallbackValue, operationName, "Circuit breaker open");
            }
        }

        try {
            // 执行操作，带超时控制
            T result = executeWithTimeout(operation, operationName);
            
            // 操作成功，处理熔断器状态
            handleOperationSuccess(operationName);
            
            return result;
            
        } catch (TimeoutException e) {
            return handleTimeout(e, fallbackValue, operationName);
        } catch (RedisConnectionFailureException e) {
            return handleRedisFailure(e, fallbackValue, operationName);
        } catch (DataAccessException e) {
            return handleRedisFailure(e, fallbackValue, operationName);
        } catch (Exception e) {
            return handleUnexpectedError(e, fallbackValue, operationName);
        }
    }

    /**
     * 执行操作，带超时控制
     */
    private <T> T executeWithTimeout(RedisOperation<T> operation, String operationName) throws Exception {
        Duration timeout = cacheProperties.getResilience().getTimeout();
        
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            // 不使用超时控制
            return operation.execute();
        }
        
        Future<T> future = timeoutExecutor.submit(() -> {
            try {
                return operation.execute();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Redis operation timeout after " + timeout.toMillis() + "ms: " + operationName);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Redis operation interrupted: " + operationName, e);
        }
    }

    /**
     * 检查并更新熔断器状态
     */
    private CircuitBreakerState checkCircuitBreakerState() {
        long currentTime = System.currentTimeMillis();
        CacheProperties.Resilience.CircuitBreaker config = cacheProperties.getResilience().getCircuitBreaker();
        
        switch (circuitBreakerState) {
            case CLOSED:
                // 正常状态，无需处理
                return CircuitBreakerState.CLOSED;
                
            case OPEN:
                // 检查是否可以进入半开状态
                long openDuration = config.getOpenDuration().toMillis();
                if (currentTime - circuitOpenTime.get() >= openDuration) {
                    log.info("Circuit breaker transitioning from OPEN to HALF_OPEN");
                    circuitBreakerState = CircuitBreakerState.HALF_OPEN;
                    halfOpenSuccesses.set(0);
                    return CircuitBreakerState.HALF_OPEN;
                }
                return CircuitBreakerState.OPEN;
                
            case HALF_OPEN:
                // 半开状态，允许部分请求通过
                return CircuitBreakerState.HALF_OPEN;
                
            default:
                return CircuitBreakerState.CLOSED;
        }
    }

    /**
     * 处理操作成功
     */
    private void handleOperationSuccess(String operationName) {
        CacheProperties.Resilience.CircuitBreaker config = cacheProperties.getResilience().getCircuitBreaker();
        
        if (!config.isEnabled()) {
            return;
        }
        
        switch (circuitBreakerState) {
            case CLOSED:
                // 重置失败计数
                if (consecutiveFailures.get() > 0) {
                    log.debug("Resetting failure count after successful operation: {}", operationName);
                    consecutiveFailures.set(0);
                }
                break;
                
            case HALF_OPEN:
                // 半开状态下成功，增加成功计数
                int successes = halfOpenSuccesses.incrementAndGet();
                log.debug("Half-open success count: {}/{}", successes, config.getHalfOpenRequests());
                
                if (successes >= config.getHalfOpenRequests()) {
                    // 达到成功阈值，关闭熔断器
                    log.info("Circuit breaker transitioning from HALF_OPEN to CLOSED after {} successful requests", successes);
                    circuitBreakerState = CircuitBreakerState.CLOSED;
                    consecutiveFailures.set(0);
                    recordCircuitBreakerMetrics(operationName, true);
                }
                break;
                
            case OPEN:
                // 不应该到达这里
                break;
        }
    }

    /**
     * 处理 Redis 故障
     */
    private <T> T handleRedisFailure(Exception e, T fallbackValue, String operationName) {
        log.error("Redis connection failed for operation: {}, error: {}", operationName, e.getMessage());
        
        recordFailure(operationName);
        
        return handleFallback(fallbackValue, operationName, "Redis connection failure");
    }

    /**
     * 处理超时
     */
    private <T> T handleTimeout(Exception e, T fallbackValue, String operationName) {
        log.error("Redis operation timeout for operation: {}, error: {}", operationName, e.getMessage());
        
        recordFailure(operationName);
        
        return handleFallback(fallbackValue, operationName, "Operation timeout");
    }

    /**
     * 处理意外错误
     */
    private <T> T handleUnexpectedError(Exception e, T fallbackValue, String operationName) {
        log.error("Unexpected error during Redis operation: {}", operationName, e);
        
        recordFailure(operationName);
        
        return handleFallback(fallbackValue, operationName, "Unexpected error");
    }

    /**
     * 记录失败并更新熔断器状态
     */
    private void recordFailure(String operationName) {
        lastFailureTime.set(System.currentTimeMillis());
        
        CacheProperties.Resilience.CircuitBreaker config = cacheProperties.getResilience().getCircuitBreaker();
        
        if (!config.isEnabled()) {
            return;
        }
        
        int failures = consecutiveFailures.incrementAndGet();
        log.warn("Consecutive failures: {}/{}", failures, config.getFailureThreshold());
        
        switch (circuitBreakerState) {
            case CLOSED:
                // 检查是否达到失败阈值
                if (failures >= config.getFailureThreshold()) {
                    log.error("Circuit breaker opening after {} consecutive failures", failures);
                    circuitBreakerState = CircuitBreakerState.OPEN;
                    circuitOpenTime.set(System.currentTimeMillis());
                    recordCircuitBreakerMetrics(operationName, false);
                }
                break;
                
            case HALF_OPEN:
                // 半开状态下失败，立即打开熔断器
                log.error("Circuit breaker reopening after failure in HALF_OPEN state");
                circuitBreakerState = CircuitBreakerState.OPEN;
                circuitOpenTime.set(System.currentTimeMillis());
                halfOpenSuccesses.set(0);
                recordCircuitBreakerMetrics(operationName, false);
                break;
                
            case OPEN:
                // 已经打开，无需处理
                break;
        }
    }

    /**
     * 处理降级逻辑
     */
    private <T> T handleFallback(T fallbackValue, String operationName, String reason) {
        if (cacheProperties.getResilience().isFallbackEnabled()) {
            log.warn("Using fallback for operation: {}, reason: {}", operationName, reason);
            return fallbackValue;
        }
        
        throw new CacheConnectionException("Redis operation failed: " + operationName + ", reason: " + reason);
    }

    /**
     * 记录熔断器指标
     */
    private void recordCircuitBreakerMetrics(String operationName, boolean recovered) {
        if (recovered) {
            log.info("Circuit breaker recovered for operation: {}", operationName);
        } else {
            log.warn("Circuit breaker triggered for operation: {}", operationName);
        }
    }

    /**
     * 自动恢复检测
     * 定期检查 Redis 连接状态
     */
    @Scheduled(fixedDelayString = "#{@cacheProperties.resilience.autoRecovery.checkInterval.toMillis()}")
    public void autoRecoveryCheck() {
        if (!cacheProperties.getResilience().getAutoRecovery().isEnabled()) {
            return;
        }
        
        if (circuitBreakerState == CircuitBreakerState.OPEN) {
            log.debug("Auto-recovery check: Circuit breaker is OPEN, checking Redis availability");
            
            try {
                // 尝试执行简单的 ping 操作
                redisTemplate.execute((RedisCallback<String>) connection -> {
                    return connection.ping();
                });
                
                log.info("Auto-recovery check: Redis is available, but circuit breaker will open naturally after timeout");
            } catch (Exception e) {
                log.debug("Auto-recovery check: Redis still unavailable: {}", e.getMessage());
            }
        }
    }

    /**
     * Redis 操作函数式接口
     */
    @FunctionalInterface
    private interface RedisOperation<T> {
        T execute() throws Exception;
    }

    /**
     * 检查 Redis 是否可用
     *
     * @return true 如果 Redis 可用
     */
    public boolean isRedisAvailable() {
        return circuitBreakerState == CircuitBreakerState.CLOSED;
    }

    /**
     * 获取熔断器状态
     */
    public String getCircuitBreakerState() {
        return circuitBreakerState.name();
    }

    /**
     * 获取连续失败次数
     */
    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    /**
     * 手动重置熔断器
     * 主要用于测试或手动恢复
     */
    public void resetCircuitBreaker() {
        log.info("Manually resetting circuit breaker");
        circuitBreakerState = CircuitBreakerState.CLOSED;
        consecutiveFailures.set(0);
        halfOpenSuccesses.set(0);
        lastFailureTime.set(0);
        circuitOpenTime.set(0);
    }

    /**
     * 手动打开熔断器
     * 主要用于测试
     */
    public void openCircuitBreaker() {
        log.warn("Manually opening circuit breaker");
        circuitBreakerState = CircuitBreakerState.OPEN;
        circuitOpenTime.set(System.currentTimeMillis());
    }
    
    /**
     * 关闭资源
     */
    public void shutdown() {
        if (timeoutExecutor != null && !timeoutExecutor.isShutdown()) {
            log.info("Shutting down timeout executor");
            timeoutExecutor.shutdown();
            try {
                if (!timeoutExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    timeoutExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                timeoutExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // ========== String 操作 ==========

    /**
     * 设置缓存
     */
    public void set(String key, Object value) {
        executeWithFallback(() -> {
            redisTemplate.opsForValue().set(key, value);
            return null;
        }, null, "set");
    }

    /**
     * 设置缓存并指定过期时间
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        executeWithFallback(() -> {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            return null;
        }, null, "set");
    }

    /**
     * 设置缓存并指定过期时间（秒）
     */
    public void set(String key, Object value, long seconds) {
        executeWithFallback(() -> {
            redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
            return null;
        }, null, "set");
    }
 
    /**
     * 使用 SCAN 命令安全地获取所有符合pattern的key集合
     * 避免阻塞Redis，推荐使用此方法替代keys()
     *
     * @param pattern 键模式（支持通配符 * 和 ?）
     * @param count 每次扫描的键数量（默认100）
     * @return 匹配的键集合
     */
    public Set<String> scan(String pattern, int count) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return Collections.emptySet();
        }

        if (count <= 0) {
            count = 100; // 默认值
        }

        long startTime = System.currentTimeMillis();

        // 创建最终副本以供 lambda 使用
        final String finalPattern = pattern;
        final int finalCount = count;

        // 使用数组来存储可变状态
        final Set<String>[] keys = new HashSet[]{new HashSet<>()};
        final long[] totalScanned = {0};

        return executeWithFallback(() -> {
            // 使用 RedisCallback 实现 SCAN 命令
            ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(finalPattern)
                .count(finalCount)
                .build();

            try {
                redisTemplate.execute((RedisCallback<Void>) connection -> {
                    try (Cursor<byte[]> cursor = connection.scan(scanOptions)) {
                        while (cursor.hasNext()) {
                            byte[] keyBytes = cursor.next();
                            keys[0].add(new String(keyBytes));
                            totalScanned[0]++;

                            // 每扫描1000个键输出一次日志，避免日志过多
                            if (totalScanned[0] % 1000 == 0) {
                                log.debug("SCAN progress: {} keys scanned for pattern: {}", totalScanned[0], finalPattern);
                            }
                        }
                        return null;
                    }
                });

                long duration = System.currentTimeMillis() - startTime;
                log.info("SCAN completed successfully: {} keys found in {} ms, pattern: {}",
                    keys[0].size(), duration, finalPattern);

                return keys[0];

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("SCAN failed after {} ms for pattern: {}, falling back to keys()", duration, finalPattern, e);

                // SCAN 失败时降级到 keys()
                Set<String> fallbackKeys = redisTemplate.keys(finalPattern);
                if (fallbackKeys != null) {
                    log.warn("FALLBACK: keys() returned {} keys for pattern: {} (this may block Redis!)",
                        fallbackKeys.size(), finalPattern);
                    return fallbackKeys;
                }

                return Collections.emptySet();
            }
        }, Collections.emptySet(), "scan");
    }

    /**
     * 使用 SCAN 命令获取所有符合pattern的key集合（使用默认count=100）
     *
     * @param pattern 键模式
     * @return 匹配的键集合
     */
    public Set<String> scan(String pattern) {
        return scan(pattern, 100);
    }

    /**
     * 获取所有符合pattern的key集合
     * ⚠️ 已废弃：此方法会阻塞Redis单线程执行，在大规模key空间下可能导致性能问题
     *
     * @deprecated 使用 {@link #scan(String)} 或 {@link #scan(String, int)} 替代
     * @param pattern 键模式（支持通配符 * 和 ?）
     * @return 匹配的键集合
     */
    @Deprecated
    public Set<String> keys(String pattern) {
        log.warn("⚠️ 使用已废弃的keys()方法，建议使用scan()替代。pattern: {}", pattern);
        return executeWithFallback(() -> redisTemplate.keys(pattern), Collections.emptySet(), "keys");
    }

    /**
     * 获取缓存，返回List<Object>
     */
    @SuppressWarnings("unchecked")
    public List<Object> getList(String key) {
        return executeWithFallback(() -> {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof List) {
                return (List<Object>) value;
            }
            return null;
        }, null, "getList");
    }

    /**
     * 获取缓存
     */
    public Object get(String key) {
        return executeWithFallback(() -> redisTemplate.opsForValue().get(key), null, "get");
    }

    /**
     * 删除缓存
     */
    public Boolean delete(String key) {
        return executeWithFallback(() -> redisTemplate.delete(key), false, "delete");
    }

    /**
     * 批量删除缓存
     */
    public Long delete(Collection<String> keys) {
        return executeWithFallback(() -> redisTemplate.delete(keys), 0L, "delete");
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return executeWithFallback(() -> redisTemplate.expire(key, timeout, unit), false, "expire");
    }

    /**
     * 获取过期时间
     */
    public Long getExpire(String key) {
        return executeWithFallback(() -> redisTemplate.getExpire(key), -1L, "getExpire");
    }

    /**
     * 判断key是否存在
     */
    public Boolean hasKey(String key) {
        return executeWithFallback(() -> redisTemplate.hasKey(key), false, "hasKey");
    }

    /**
     * 递增
     */
    public Long increment(String key, long delta) {
        return executeWithFallback(() -> redisTemplate.opsForValue().increment(key, delta), 0L, "increment");
    }

    /**
     * 递减
     */
    public Long decrement(String key, long delta) {
        return executeWithFallback(() -> redisTemplate.opsForValue().decrement(key, delta), 0L, "decrement");
    }

    // ========== Hash 操作 ==========

    /**
     * 获取Hash中的数据
     */
    public Object hGet(String key, String hashKey) {
        return executeWithFallback(() -> redisTemplate.opsForHash().get(key, hashKey), null, "hGet");
    }

    /**
     * 获取Hash中的所有数据
     */
    public Map<Object, Object> hGetAll(String key) {
        return executeWithFallback(() -> redisTemplate.opsForHash().entries(key), Collections.emptyMap(), "hGetAll");
    }

    /**
     * 设置Hash中的数据
     */
    public void hSet(String key, String hashKey, Object value) {
        executeWithFallback(() -> {
            redisTemplate.opsForHash().put(key, hashKey, value);
            return null;
        }, null, "hSet");
    }

    /**
     * 批量设置Hash数据
     */
    public void hSetAll(String key, Map<String, Object> map) {
        executeWithFallback(() -> {
            redisTemplate.opsForHash().putAll(key, map);
            return null;
        }, null, "hSetAll");
    }

    /**
     * 删除Hash中的数据
     */
    public Long hDelete(String key, Object... hashKeys) {
        return executeWithFallback(() -> redisTemplate.opsForHash().delete(key, hashKeys), 0L, "hDelete");
    }

    /**
     * 判断Hash中是否存在某个key
     */
    public Boolean hHasKey(String key, String hashKey) {
        return executeWithFallback(() -> redisTemplate.opsForHash().hasKey(key, hashKey), false, "hHasKey");
    }

    // ========== Set 操作 ==========

    /**
     * 向Set中添加元素
     */
    public Long sAdd(String key, Object... values) {
        return executeWithFallback(() -> redisTemplate.opsForSet().add(key, values), 0L, "sAdd");
    }

    /**
     * 获取Set中的所有元素
     */
    public Set<Object> sMembers(String key) {
        return executeWithFallback(() -> redisTemplate.opsForSet().members(key), Collections.emptySet(), "sMembers");
    }

    /**
     * 判断Set中是否存在某个元素
     */
    public Boolean sIsMember(String key, Object value) {
        return executeWithFallback(() -> redisTemplate.opsForSet().isMember(key, value), false, "sIsMember");
    }

    /**
     * 获取Set的大小
     */
    public Long sSize(String key) {
        return executeWithFallback(() -> redisTemplate.opsForSet().size(key), 0L, "sSize");
    }

    /**
     * 移除Set中的元素
     */
    public Long sRemove(String key, Object... values) {
        return executeWithFallback(() -> redisTemplate.opsForSet().remove(key, values), 0L, "sRemove");
    }

    // ========== List 操作 ==========

    /**
     * 向List右侧添加元素
     */
    public Long lPush(String key, Object value) {
        return executeWithFallback(() -> redisTemplate.opsForList().rightPush(key, value), 0L, "lPush");
    }

    /**
     * 向List左侧添加元素
     */
    public Long lLeftPush(String key, Object value) {
        return executeWithFallback(() -> redisTemplate.opsForList().leftPush(key, value), 0L, "lLeftPush");
    }

    /**
     * 获取List中的元素
     */
    public List<Object> lRange(String key, long start, long end) {
        return executeWithFallback(() -> redisTemplate.opsForList().range(key, start, end), Collections.emptyList(), "lRange");
    }

    /**
     * 获取List的大小
     */
    public Long lSize(String key) {
        return executeWithFallback(() -> redisTemplate.opsForList().size(key), 0L, "lSize");
    }

    /**
     * 根据索引获取List中的元素
     */
    public Object lIndex(String key, long index) {
        return executeWithFallback(() -> redisTemplate.opsForList().index(key, index), null, "lIndex");
    }

    /**
     * 移除List中的元素
     */
    public Long lRemove(String key, long count, Object value) {
        return executeWithFallback(() -> redisTemplate.opsForList().remove(key, count, value), 0L, "lRemove");
    }
}
