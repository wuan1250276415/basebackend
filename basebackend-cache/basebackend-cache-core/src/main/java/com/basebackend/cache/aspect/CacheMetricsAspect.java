package com.basebackend.cache.aspect;

import com.basebackend.cache.annotation.CacheEvict;
import com.basebackend.cache.annotation.CachePut;
import com.basebackend.cache.annotation.Cacheable;
import com.basebackend.cache.config.CacheProperties;
import com.basebackend.cache.metrics.CacheMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 缓存指标切面
 * 自动记录缓存操作的性能指标
 * 
 * 功能：
 * - 记录缓存命中/未命中
 * - 记录操作延迟
 * - 记录操作成功/失败
 * 
 * 注意：此切面的优先级低于 CacheAspect，确保在缓存操作之后执行
 */
@Slf4j
@Aspect
@Component
@Order(100) // 较低优先级，在 CacheAspect 之后执行
@RequiredArgsConstructor
public class CacheMetricsAspect {
    
    private final CacheMetricsService metricsService;
    private final CacheProperties cacheProperties;
    
    /**
     * 拦截 @Cacheable 注解，记录缓存查询指标
     */
    @Around("@annotation(cacheable)")
    public Object recordCacheableMetrics(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
        // 检查指标是否启用
        if (!isMetricsEnabled()) {
            return joinPoint.proceed();
        }
        
        String cacheName = getCacheName(cacheable.cacheName(), joinPoint);
        long startTime = System.currentTimeMillis();
        boolean success = true;
        
        try {
            Object result = joinPoint.proceed();
            
            // 记录命中或未命中
            // 注意：这里无法准确判断是否命中，因为 CacheAspect 已经处理过了
            // 我们只记录操作延迟
            
            return result;
        } catch (Throwable e) {
            success = false;
            String errorMessage = e.getMessage();
            long latency = System.currentTimeMillis() - startTime;
            metricsService.recordLatency(cacheName, "GET", latency, success, errorMessage);
            throw e;
        } finally {
            if (success) {
                long latency = System.currentTimeMillis() - startTime;
                metricsService.recordLatency(cacheName, "GET", latency, success, null);
                
                log.debug("Recorded cache GET metrics: cache={}, latency={}ms, success={}", 
                         cacheName, latency, success);
            }
        }
    }
    
    /**
     * 拦截 @CachePut 注解，记录缓存更新指标
     */
    @Around("@annotation(cachePut)")
    public Object recordCachePutMetrics(ProceedingJoinPoint joinPoint, CachePut cachePut) throws Throwable {
        // 检查指标是否启用
        if (!isMetricsEnabled()) {
            return joinPoint.proceed();
        }
        
        String cacheName = getCacheName(cachePut.cacheName(), joinPoint);
        long startTime = System.currentTimeMillis();
        boolean success = true;
        
        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            success = false;
            throw e;
        } finally {
            long latency = System.currentTimeMillis() - startTime;
            metricsService.recordSet(cacheName, latency, success);
            
            log.debug("Recorded cache PUT metrics: cache={}, latency={}ms, success={}", 
                     cacheName, latency, success);
        }
    }
    
    /**
     * 拦截 @CacheEvict 注解，记录缓存淘汰指标
     */
    @Around("@annotation(cacheEvict)")
    public Object recordCacheEvictMetrics(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict) throws Throwable {
        // 检查指标是否启用
        if (!isMetricsEnabled()) {
            return joinPoint.proceed();
        }
        
        String cacheName = getCacheName(cacheEvict.cacheName(), joinPoint);
        long startTime = System.currentTimeMillis();
        boolean success = true;
        
        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            success = false;
            throw e;
        } finally {
            long latency = System.currentTimeMillis() - startTime;
            metricsService.recordEviction(cacheName, latency, success);
            
            log.debug("Recorded cache EVICT metrics: cache={}, latency={}ms, success={}", 
                     cacheName, latency, success);
        }
    }
    
    /**
     * 获取缓存名称
     * 如果注解中未指定，使用类名
     */
    private String getCacheName(String annotationCacheName, ProceedingJoinPoint joinPoint) {
        if (annotationCacheName != null && !annotationCacheName.isEmpty()) {
            return annotationCacheName;
        }
        
        // 使用类名作为默认缓存名称
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return method.getDeclaringClass().getSimpleName();
    }
    
    /**
     * 检查指标是否启用
     */
    private boolean isMetricsEnabled() {
        return cacheProperties.getMetrics().isEnabled();
    }
}
