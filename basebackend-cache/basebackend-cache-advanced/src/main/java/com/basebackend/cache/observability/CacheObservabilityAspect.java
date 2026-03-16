package com.basebackend.cache.observability;

import com.basebackend.observability.slo.annotation.SloMonitored;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Cache 模块可观测性集成切面
 * <p>
 * 使用 Observation API 为缓存操作自动添加追踪和指标监控。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@ConditionalOnClass(name = "io.micrometer.observation.ObservationRegistry")
@ConditionalOnProperty(prefix = "basebackend.cache.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheObservabilityAspect {

    private final ObservationRegistry observationRegistry;

    public CacheObservabilityAspect(
            @Autowired(required = false) ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry != null
                ? observationRegistry : ObservationRegistry.NOOP;
    }

    /**
     * 拦截所有 CacheService 的方法，通过 Observation API 添加追踪和指标
     */
    @Around("execution(* com.basebackend.cache.service.CacheService.*(..)) || " +
            "execution(* com.basebackend.cache.service.CacheServiceImpl.*(..))")
    @SloMonitored(sloName = "cache-operations", service = "cache-service")
    public Object traceCacheOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String operation = determineCacheOperation(methodName);

        Observation observation = Observation.createNotStarted(
                        "cache.operation", observationRegistry)
                .lowCardinalityKeyValue("cache.operation", operation)
                .lowCardinalityKeyValue("cache.method", methodName)
                .contextualName("cache." + operation);

        // 提取缓存 key（如果是第一个参数）
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof String key) {
            observation.lowCardinalityKeyValue("cache.key.namespace", extractKeyNamespace(key));
        }

        return observation.observeChecked(() -> {
            Object result = joinPoint.proceed();
            if ("get".equals(operation)) {
                boolean hit = result != null;
                observation.lowCardinalityKeyValue("cache.hit", String.valueOf(hit));
            }
            return result;
        });
    }

    /**
     * 拦截分布式锁操作
     */
    @Around("execution(* com.basebackend.cache.lock.DistributedLockService.*(..))")
    public Object traceDistributedLock(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();

        Observation observation = Observation.createNotStarted(
                        "cache.lock", observationRegistry)
                .lowCardinalityKeyValue("lock.method", methodName)
                .contextualName("cache.lock." + methodName);

        // 提取锁 key
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof String key) {
            observation.lowCardinalityKeyValue("lock.key.namespace", extractKeyNamespace(key));
        }

        return observation.observeChecked(() -> {
            Object result = joinPoint.proceed();
            if (methodName.contains("tryLock") && result instanceof Boolean acquired) {
                observation.lowCardinalityKeyValue("lock.acquired", String.valueOf(acquired));
            }
            return result;
        });
    }

    private String determineCacheOperation(String methodName) {
        if (methodName.startsWith("get") || methodName.contains("find")) {
            return "get";
        } else if (methodName.startsWith("put") || methodName.startsWith("set") || methodName.contains("save")) {
            return "put";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove") || methodName.contains("evict")) {
            return "delete";
        } else if (methodName.contains("exist") || methodName.contains("has")) {
            return "exists";
        } else {
            return "unknown";
        }
    }

    private String extractKeyNamespace(String key) {
        if (!StringUtils.hasText(key)) {
            return "unknown";
        }
        String normalizedKey = key.trim();
        int separatorIndex = normalizedKey.indexOf(':');
        if (separatorIndex < 0) {
            separatorIndex = normalizedKey.indexOf('|');
        }
        String namespace = separatorIndex > 0 ? normalizedKey.substring(0, separatorIndex) : normalizedKey;
        return namespace.length() > 64 ? namespace.substring(0, 64) : namespace;
    }
}
