package com.basebackend.logging.cache;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;

/**
 * 热点日志缓存AOP切面
 *
 * 拦截标注了@HotLoggable注解的方法，实现自动缓存管理。
 * 支持读透、写透、写回、失效等多种缓存策略。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Aspect
@Component
@Order(10)  // 设置优先级，确保在其他切面之前执行
public class HotLogCacheAspect {

    /**
     * Redis热点缓存
     */
    private final RedisHotLogCache cache;

    /**
     * 配置属性
     */
    private final HotLogCacheProperties properties;

    /**
     * 构造函数
     *
     * @param cache       Redis缓存
     * @param properties 配置属性
     */
    public HotLogCacheAspect(RedisHotLogCache cache, HotLogCacheProperties properties) {
        this.cache = cache;
        this.properties = properties;
    }

    /**
     * 环绕通知：拦截@HotLoggable注解的方法
     *
     * @param pjp 连接点
     * @param hot 注解
     * @return 方法执行结果
     * @throws Throwable 执行异常
     */
    @Around("@annotation(hot)")
    public Object around(ProceedingJoinPoint pjp, HotLoggable hot) throws Throwable {
        // 如果缓存未启用，直接执行方法
        if (!properties.isEnabled()) {
            return pjp.proceed();
        }

        // 获取方法签名
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();

        // 解析缓存键
        String cacheKey = resolveCacheKey(pjp, method, hot);

        // 解析TTL和策略
        Duration ttl = cache.resolveTtl(hot.ttlSeconds());
        HotLoggable.CacheStrategy strategy = hot.strategy();
        int hotThreshold = hot.hotThreshold();

        // 判断是否为读操作
        boolean isRead = strategy == HotLoggable.CacheStrategy.READ_THROUGH;

        // 判断是否为写操作
        boolean isWrite = strategy == HotLoggable.CacheStrategy.WRITE_THROUGH
                || strategy == HotLoggable.CacheStrategy.WRITE_BACK;

        // 判断是否为失效操作
        boolean invalidate = strategy == HotLoggable.CacheStrategy.INVALIDATE;

        // 读透策略：先查缓存，命中则返回
        if (isRead) {
            Optional<Object> cachedResult = cache.get(cacheKey, Object.class);
            if (cachedResult.isPresent()) {
                return cachedResult.get();
            }
        }

        // 执行原方法
        Object result;
        try {
            result = pjp.proceed();
        } catch (Throwable ex) {
            // 执行失败且设置了异常失效，则删除缓存
            if (hot.invalidateOnException()) {
                cache.evict(cacheKey);
            }
            throw ex;  // 重新抛出异常
        }

        // 判断是否应该提升为热点数据
        boolean shouldPromote = cache.shouldPromote(cacheKey, hotThreshold) || hot.preload();

        // 失效策略：删除缓存后直接返回结果
        if (invalidate) {
            cache.evict(cacheKey);
            return result;
        }

        // 读操作未命中：将结果放入缓存
        // 写操作：将结果放入缓存
        if (shouldPromote && (isRead || isWrite)) {
            cache.put(cacheKey, result, ttl);
        }

        return result;
    }

    /**
     * 解析缓存键
     *
     * @param pjp   连接点
     * @param method 方法
     * @param hot   注解
     * @return 缓存键
     */
    private String resolveCacheKey(ProceedingJoinPoint pjp, Method method, HotLoggable hot) {
        // 优先使用注解中指定的键
        if (StringUtils.hasText(hot.cacheKey())) {
            return hot.cacheKey();
        }

        // 生成默认键（类名#方法名|参数哈希）
        return cache.defaultKey(pjp.getTarget(), method.getName(), pjp.getArgs());
    }
}
