package com.basebackend.common.ratelimit.aspect;

import com.basebackend.common.ratelimit.RateLimit;
import com.basebackend.common.ratelimit.RateLimitExceededException;
import com.basebackend.common.ratelimit.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

@Aspect
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimiter rateLimiter;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = resolveKey(joinPoint, rateLimit);

        if (!rateLimiter.tryAcquire(key, rateLimit.limit(), rateLimit.window())) {
            String fallback = rateLimit.fallbackMethod();
            if (!fallback.isEmpty()) {
                return invokeFallback(joinPoint, fallback);
            }
            throw new RateLimitExceededException(rateLimit.message());
        }

        return joinPoint.proceed();
    }

    private String resolveKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        if (!rateLimit.key().isEmpty()) {
            return rateLimit.key();
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringTypeName() + "#" + signature.getName();
    }

    private Object invokeFallback(ProceedingJoinPoint joinPoint, String fallbackMethod) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object target = joinPoint.getTarget();
        Method method = target.getClass().getDeclaredMethod(fallbackMethod, signature.getParameterTypes());
        method.setAccessible(true);
        return method.invoke(target, joinPoint.getArgs());
    }
}
