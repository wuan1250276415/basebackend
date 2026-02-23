package com.basebackend.common.lock.aspect;

import com.basebackend.common.lock.annotation.DistributedLock;
import com.basebackend.common.lock.exception.LockAcquisitionException;
import com.basebackend.common.lock.config.LockProperties;
import com.basebackend.common.lock.provider.DistributedLockProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;

/**
 * 分布式锁 AOP 切面
 * <p>
 * 解析 {@link DistributedLock} 注解，自动处理锁的获取与释放。
 * 支持 SpEL 表达式动态生成锁 key。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final DistributedLockProvider lockProvider;
    private final LockProperties lockProperties;

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String lockKey = buildLockKey(joinPoint, distributedLock);

        boolean acquired = false;
        try {
            acquired = lockProvider.tryLock(
                    lockKey,
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
            );

            if (!acquired) {
                log.warn("获取分布式锁失败, key={}", lockKey);
                throw new LockAcquisitionException(distributedLock.errorMessage());
            }

            log.debug("获取分布式锁成功, key={}", lockKey);
            return joinPoint.proceed();
        } finally {
            if (acquired) {
                try {
                    lockProvider.unlock(lockKey);
                    log.debug("释放分布式锁成功, key={}", lockKey);
                } catch (Exception e) {
                    log.error("释放分布式锁异常, key={}", lockKey, e);
                }
            }
        }
    }

    /**
     * 构建完整的锁 key
     * <p>
     * 格式: {全局前缀}{注解前缀}{SpEL解析结果}
     * </p>
     */
    private String buildLockKey(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) {
        String spelKey = resolveSpelKey(joinPoint, distributedLock.key());
        return lockProperties.getKeyPrefix() + distributedLock.prefix() + spelKey;
    }

    /**
     * 解析 SpEL 表达式
     */
    private String resolveSpelKey(ProceedingJoinPoint joinPoint, String spelExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                joinPoint.getTarget(), method, joinPoint.getArgs(), NAME_DISCOVERER
        );

        Object value = PARSER.parseExpression(spelExpression).getValue(context);
        return value != null ? value.toString() : "null";
    }
}
