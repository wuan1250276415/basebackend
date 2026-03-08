package com.basebackend.cache.ratelimit;

import com.basebackend.cache.annotation.RateLimit;
import com.basebackend.cache.config.CacheProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 限流切面
 * 拦截 @RateLimit 注解的方法，执行分布式限流检查
 * 采用 fail-open 策略：Redis 不可用时放行请求
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    private final RateLimitService rateLimitService;
    private final CacheProperties cacheProperties;
    private final MeterRegistry meterRegistry;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public RateLimitAspect(
            RateLimitService rateLimitService,
            CacheProperties cacheProperties,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        this.rateLimitService = rateLimitService;
        this.cacheProperties = cacheProperties;
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(rateLimit)")
    public Object handleRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        CacheProperties.RateLimiter config = cacheProperties.getRateLimiter();
        if (!config.isEnabled()) {
            return joinPoint.proceed();
        }

        String key = resolveKey(rateLimit, joinPoint);
        String keyScope = resolveKeyScope(key);
        long rate = rateLimit.rate();
        long interval = rateLimit.interval();
        TimeUnit timeUnit = rateLimit.timeUnit();
        RateType mode = rateLimit.mode();

        try {
            boolean acquired = rateLimitService.tryAcquire(key, rate, interval, timeUnit, mode);

            recordMetric(keyScope, acquired);

            if (!acquired) {
                log.warn("Rate limit exceeded: key={}, rate={}/{} {}", key, rate, interval, timeUnit);
                throw new RateLimitExceededException(key, timeUnit.toMillis(interval));
            }

            return joinPoint.proceed();
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            // Fail-open: Redis 不可用时放行
            if (config.isFailOpen()) {
                log.warn("Rate limiter fail-open: key={}, error={}", key, e.getMessage());
                recordFailOpen(keyScope);
                return joinPoint.proceed();
            }
            throw e;
        }
    }

    private String resolveKey(RateLimit rateLimit, ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        String keyExpression = rateLimit.key();
        if (!StringUtils.hasText(keyExpression)) {
            return method.getDeclaringClass().getSimpleName() + ":" + method.getName();
        }

        if (keyExpression.contains("#")) {
            try {
                MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                        joinPoint.getTarget(), method, joinPoint.getArgs(), parameterNameDiscoverer);
                Object value = parser.parseExpression(keyExpression).getValue(context);
                return value != null ? value.toString() : keyExpression;
            } catch (Exception e) {
                log.error("Failed to parse SpEL rate limit key: {}", keyExpression, e);
                return keyExpression;
            }
        }

        return keyExpression;
    }

    private void recordMetric(String keyScope, boolean acquired) {
        if (meterRegistry == null) {
            return;
        }
        String result = acquired ? "allowed" : "rejected";
        Counter.builder("cache.ratelimit.acquired")
                .tag("key_scope", keyScope)
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    private void recordFailOpen(String keyScope) {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder("cache.ratelimit.failopen")
                .tag("key_scope", keyScope)
                .register(meterRegistry)
                .increment();
    }

    private String resolveKeyScope(String key) {
        if (!StringUtils.hasText(key)) {
            return "unknown";
        }
        String normalizedKey = key.trim();
        int separatorIndex = normalizedKey.indexOf(':');
        if (separatorIndex < 0) {
            separatorIndex = normalizedKey.indexOf('|');
        }
        String scope = separatorIndex > 0 ? normalizedKey.substring(0, separatorIndex) : normalizedKey;
        return scope.length() > 64 ? scope.substring(0, 64) : scope;
    }
}
