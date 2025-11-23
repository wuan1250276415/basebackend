package com.basebackend.cache.aspect;

import com.basebackend.cache.annotation.DistributedLock;
import com.basebackend.cache.exception.CacheLockException;
import com.basebackend.cache.lock.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁切面
 * 处理 @DistributedLock 注解
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final DistributedLockService lockService;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(com.basebackend.cache.annotation.DistributedLock)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock lockAnnotation = method.getAnnotation(DistributedLock.class);

        // 解析锁键
        String lockKey = parseLockKey(lockAnnotation.key(), method, joinPoint.getArgs());
        long waitTime = lockAnnotation.waitTime();
        long leaseTime = lockAnnotation.leaseTime();
        TimeUnit timeUnit = lockAnnotation.timeUnit();
        DistributedLock.LockType lockType = lockAnnotation.lockType();
        boolean throwException = lockAnnotation.throwException();

        log.debug("Attempting to acquire {} lock: {}", lockType, lockKey);

        RLock lock = null;
        boolean acquired = false;

        try {
            // 根据锁类型获取相应的锁
            switch (lockType) {
                case FAIR:
                    lock = lockService.getFairLock(lockKey);
                    acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
                    break;
                case READ:
                    acquired = lockService.tryReadLock(lockKey, waitTime, leaseTime, timeUnit);
                    break;
                case WRITE:
                    acquired = lockService.tryWriteLock(lockKey, waitTime, leaseTime, timeUnit);
                    break;
                case REENTRANT:
                default:
                    acquired = lockService.tryLock(lockKey, waitTime, leaseTime, timeUnit);
                    break;
            }

            if (!acquired) {
                String errorMsg = String.format("Failed to acquire %s lock: %s within %d %s",
                        lockType, lockKey, waitTime, timeUnit);
                log.warn(errorMsg);
                if (throwException) {
                    throw new CacheLockException(errorMsg);
                }
                return null;
            }

            // 执行目标方法
            log.debug("Lock acquired, executing method: {}", method.getName());
            return joinPoint.proceed();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMsg = String.format("Interrupted while acquiring lock: %s", lockKey);
            log.error(errorMsg, e);
            throw new CacheLockException(errorMsg, e);
        } finally {
            // 释放锁
            if (acquired) {
                try {
                    switch (lockType) {
                        case READ:
                            lockService.unlockRead(lockKey);
                            break;
                        case WRITE:
                            lockService.unlockWrite(lockKey);
                            break;
                        case FAIR:
                            if (lock != null && lock.isHeldByCurrentThread()) {
                                lock.unlock();
                            }
                            break;
                        case REENTRANT:
                        default:
                            lockService.unlock(lockKey);
                            break;
                    }
                    log.debug("Lock released: {}", lockKey);
                } catch (Exception e) {
                    log.error("Error releasing lock: {}", lockKey, e);
                }
            }
        }
    }

    /**
     * 解析锁键，支持 SpEL 表达式
     */
    private String parseLockKey(String keyExpression, Method method, Object[] args) {
        if (!keyExpression.contains("#")) {
            // 不包含 SpEL 表达式，直接返回
            return keyExpression;
        }

        try {
            // 创建 SpEL 上下文
            EvaluationContext context = new StandardEvaluationContext();

            // 获取方法参数名
            String[] parameterNames = signature(method);
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }

            // 解析表达式
            Expression expression = parser.parseExpression(keyExpression);
            Object value = expression.getValue(context);
            return value != null ? value.toString() : keyExpression;
        } catch (Exception e) {
            log.error("Failed to parse lock key expression: {}", keyExpression, e);
            return keyExpression;
        }
    }

    /**
     * 获取方法参数名
     */
    private String[] signature(Method method) {
        // 简化实现：使用参数索引作为名称
        // 在实际应用中，可以使用 Spring 的 ParameterNameDiscoverer
        int paramCount = method.getParameterCount();
        String[] names = new String[paramCount];
        for (int i = 0; i < paramCount; i++) {
            names[i] = "arg" + i;
        }
        return names;
    }
}
