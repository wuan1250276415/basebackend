package com.basebackend.cache.aspect;

import com.basebackend.cache.annotation.CacheEvict;
import com.basebackend.cache.annotation.CachePut;
import com.basebackend.cache.annotation.Cacheable;
import com.basebackend.cache.config.CacheProperties;
import com.basebackend.cache.manager.MultiLevelCacheManager;
import com.basebackend.cache.service.RedisService;
import com.basebackend.cache.util.CacheKeyGenerator;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 缓存切面
 * 处理 @Cacheable、@CacheEvict、@CachePut 注解
 * 支持 SpEL 表达式解析和条件判断
 */
@Slf4j
@Aspect
@Component
public class CacheAspect {
    
    private final RedisService redisService;
    private final CacheKeyGenerator keyGenerator;
    private final CacheProperties cacheProperties;
    private final MultiLevelCacheManager multiLevelCacheManager;
    
    /**
     * SpEL 表达式解析器
     */
    private final ExpressionParser parser = new SpelExpressionParser();
    
    /**
     * 参数名称发现器
     */
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    
    public CacheAspect(
            RedisService redisService,
            CacheKeyGenerator keyGenerator,
            CacheProperties cacheProperties,
            @Autowired(required = false) MultiLevelCacheManager multiLevelCacheManager) {
        this.redisService = redisService;
        this.keyGenerator = keyGenerator;
        this.cacheProperties = cacheProperties;
        this.multiLevelCacheManager = multiLevelCacheManager;
    }
    
    /**
     * 处理 @Cacheable 注解
     * 先查询缓存，如果缓存不存在则执行方法并缓存结果
     */
    @Around("@annotation(cacheable)")
    public Object handleCacheable(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object target = joinPoint.getTarget();
        Object[] args = joinPoint.getArgs();
        
        // 创建 SpEL 上下文
        EvaluationContext context = createEvaluationContext(method, args, target, null);
        
        // 检查条件表达式
        if (!evaluateCondition(cacheable.condition(), context)) {
            log.debug("Condition not met for @Cacheable, executing method directly");
            return joinPoint.proceed();
        }
        
        // 解析缓存键
        String key = parseKey(cacheable.key(), cacheable.keyPrefix(), cacheable.cacheName(), 
                             target, method, args, context);
        
        // 判断是否使用多级缓存
        boolean useMultiLevel = cacheable.useMultiLevel() && 
                               cacheProperties.getMultiLevel().isEnabled() && 
                               multiLevelCacheManager != null;
        
        // 查询缓存
        Object cachedValue = null;
        if (useMultiLevel) {
            cachedValue = multiLevelCacheManager.get(key, method.getReturnType());
            log.debug("Multi-level cache lookup for key: {}, result: {}", key, cachedValue != null ? "hit" : "miss");
        } else {
            cachedValue = redisService.get(key);
            log.debug("Redis cache lookup for key: {}, result: {}", key, cachedValue != null ? "hit" : "miss");
        }
        
        // 如果缓存命中，直接返回
        if (cachedValue != null) {
            log.debug("Cache hit for key: {}", key);
            return cachedValue;
        }
        
        // 缓存未命中，执行方法
        log.debug("Cache miss for key: {}, executing method", key);
        Object result = joinPoint.proceed();
        
        // 更新上下文，添加方法返回值
        context = createEvaluationContext(method, args, target, result);
        
        // 检查 unless 条件
        if (evaluateCondition(cacheable.unless(), context)) {
            log.debug("Unless condition met for @Cacheable, not caching result");
            return result;
        }
        
        // 缓存结果
        if (result != null) {
            Duration ttl = Duration.of(cacheable.ttl(), cacheable.timeUnit().toChronoUnit());
            
            if (useMultiLevel) {
                multiLevelCacheManager.set(key, result, ttl);
                log.debug("Cached result in multi-level cache with key: {}, TTL: {}", key, ttl);
            } else {
                redisService.set(key, result, cacheable.ttl(), cacheable.timeUnit());
                log.debug("Cached result in Redis with key: {}, TTL: {}", key, ttl);
            }
        }
        
        return result;
    }
    
    /**
     * 处理 @CachePut 注解
     * 总是执行方法并更新缓存
     */
    @Around("@annotation(cachePut)")
    public Object handleCachePut(ProceedingJoinPoint joinPoint, CachePut cachePut) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object target = joinPoint.getTarget();
        Object[] args = joinPoint.getArgs();
        
        // 执行方法
        Object result = joinPoint.proceed();
        
        // 创建 SpEL 上下文（包含返回值）
        EvaluationContext context = createEvaluationContext(method, args, target, result);
        
        // 检查条件表达式
        if (!evaluateCondition(cachePut.condition(), context)) {
            log.debug("Condition not met for @CachePut, not caching result");
            return result;
        }
        
        // 检查 unless 条件
        if (evaluateCondition(cachePut.unless(), context)) {
            log.debug("Unless condition met for @CachePut, not caching result");
            return result;
        }
        
        // 解析缓存键
        String key = parseKey(cachePut.key(), cachePut.keyPrefix(), cachePut.cacheName(), 
                             target, method, args, context);
        
        // 判断是否使用多级缓存
        boolean useMultiLevel = cachePut.useMultiLevel() && 
                               cacheProperties.getMultiLevel().isEnabled() && 
                               multiLevelCacheManager != null;
        
        // 更新缓存
        if (result != null) {
            Duration ttl = Duration.of(cachePut.ttl(), cachePut.timeUnit().toChronoUnit());
            
            if (useMultiLevel) {
                multiLevelCacheManager.set(key, result, ttl);
                log.debug("Updated multi-level cache with key: {}, TTL: {}", key, ttl);
            } else {
                redisService.set(key, result, cachePut.ttl(), cachePut.timeUnit());
                log.debug("Updated Redis cache with key: {}, TTL: {}", key, ttl);
            }
        }
        
        return result;
    }
    
    /**
     * 处理 @CacheEvict 注解
     * 清除指定的缓存
     */
    @Around("@annotation(cacheEvict)")
    public Object handleCacheEvict(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object target = joinPoint.getTarget();
        Object[] args = joinPoint.getArgs();
        
        // 创建 SpEL 上下文
        EvaluationContext context = createEvaluationContext(method, args, target, null);
        
        // 如果配置为方法执行前清除缓存
        if (cacheEvict.beforeInvocation()) {
            evictCache(cacheEvict, target, method, args, context);
        }
        
        // 执行方法
        Object result = joinPoint.proceed();
        
        // 更新上下文，添加方法返回值
        context = createEvaluationContext(method, args, target, result);
        
        // 如果配置为方法执行后清除缓存（默认）
        if (!cacheEvict.beforeInvocation()) {
            evictCache(cacheEvict, target, method, args, context);
        }
        
        return result;
    }
    
    /**
     * 执行缓存清除操作
     */
    private void evictCache(CacheEvict cacheEvict, Object target, Method method, 
                           Object[] args, EvaluationContext context) {
        // 检查条件表达式
        if (!evaluateCondition(cacheEvict.condition(), context)) {
            log.debug("Condition not met for @CacheEvict, skipping cache eviction");
            return;
        }
        
        // 判断是否使用多级缓存
        boolean useMultiLevel = cacheEvict.useMultiLevel() && 
                               cacheProperties.getMultiLevel().isEnabled() && 
                               multiLevelCacheManager != null;
        
        // 如果是清除所有条目
        if (cacheEvict.allEntries()) {
            String pattern = keyGenerator.generatePatternKey(cacheEvict.keyPrefix(), cacheEvict.cacheName());
            
            if (useMultiLevel) {
                // 多级缓存不支持模式匹配清除，需要通过 Redis 实现
                long deletedCount = redisService.deleteByPattern(pattern);
                log.info("Evicted all entries matching pattern: {}, count: {}", pattern, deletedCount);
            } else {
                long deletedCount = redisService.deleteByPattern(pattern);
                log.info("Evicted all entries from Redis matching pattern: {}, count: {}", pattern, deletedCount);
            }
        } else {
            // 清除单个缓存条目
            String key = parseKey(cacheEvict.key(), cacheEvict.keyPrefix(), cacheEvict.cacheName(), 
                                 target, method, args, context);
            
            if (useMultiLevel) {
                multiLevelCacheManager.evict(key);
                log.debug("Evicted multi-level cache for key: {}", key);
            } else {
                redisService.delete(key);
                log.debug("Evicted Redis cache for key: {}", key);
            }
        }
    }
    
    /**
     * 解析缓存键
     * 支持 SpEL 表达式
     */
    private String parseKey(String keyExpression, String keyPrefix, String cacheName, 
                           Object target, Method method, Object[] args, EvaluationContext context) {
        String parsedKey = keyExpression;
        
        // 如果键表达式包含 SpEL 表达式，进行解析
        if (StringUtils.hasText(keyExpression) && keyExpression.contains("#")) {
            try {
                Expression expression = parser.parseExpression(keyExpression);
                Object value = expression.getValue(context);
                parsedKey = value != null ? value.toString() : "";
                log.debug("Parsed SpEL key expression '{}' to '{}'", keyExpression, parsedKey);
            } catch (Exception e) {
                log.error("Failed to parse SpEL key expression: {}", keyExpression, e);
                parsedKey = keyExpression;
            }
        }
        
        // 生成完整的缓存键
        return keyGenerator.generateKey(keyPrefix, cacheName, parsedKey, target, method, args);
    }
    
    /**
     * 评估条件表达式
     * 
     * @param conditionExpression 条件表达式
     * @param context SpEL 上下文
     * @return 如果条件为空或评估为 true，返回 true；否则返回 false
     */
    private boolean evaluateCondition(String conditionExpression, EvaluationContext context) {
        if (!StringUtils.hasText(conditionExpression)) {
            return true;
        }
        
        try {
            Expression expression = parser.parseExpression(conditionExpression);
            Boolean result = expression.getValue(context, Boolean.class);
            log.debug("Evaluated condition '{}' to {}", conditionExpression, result);
            return result != null && result;
        } catch (Exception e) {
            log.error("Failed to evaluate condition expression: {}", conditionExpression, e);
            return false;
        }
    }
    
    /**
     * 创建 SpEL 评估上下文
     * 
     * @param method 方法
     * @param args 方法参数
     * @param target 目标对象
     * @param result 方法返回值（可能为 null）
     * @return SpEL 评估上下文
     */
    private EvaluationContext createEvaluationContext(Method method, Object[] args, 
                                                     Object target, Object result) {
        // 使用 Spring 的 MethodBasedEvaluationContext 支持方法参数名称
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                target, method, args, parameterNameDiscoverer);
        
        // 添加方法返回值到上下文（用于 unless 条件和 @CachePut 的键解析）
        if (result != null) {
            context.setVariable("result", result);
        }
        
        return context;
    }
}
