package com.basebackend.web.aspect;

import com.basebackend.web.annotation.Idempotent;
import com.basebackend.web.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 幂等性保证切面
 * 使用 @Idempotent 注解实现幂等性控制
 *
 * @author basebackend
 * @since 2025-11-23
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final RedissonClient redissonClient;
    private final HttpServletRequest request;

    private static final String IDEMPOTENT_PREFIX = "idempotent:";
    private static final String IDEMPOTENT_LOCK_PREFIX = "idempotent:lock:";

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // 生成幂等性key
        String idempotentKey = generateIdempotentKey(joinPoint, idempotent);

        // 检查是否已经处理过
        Object existingResult = checkAndGetResult(idempotentKey, idempotent);
        if (existingResult != null) {
            log.info("幂等性检查通过，返回缓存结果: {}", idempotentKey);
            return existingResult;
        }

        // 获取分布式锁
        String lockKey = IDEMPOTENT_LOCK_PREFIX + idempotentKey;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean lockAcquired = lock.tryLock(0, 5, TimeUnit.SECONDS);

            if (!lockAcquired) {
                // 无法获取锁，可能正在处理
                if (idempotent.strategy() == Idempotent.Strategy.WAIT) {
                    log.info("等待幂等性处理完成: {}", idempotentKey);
                    // 等待锁释放
                    lock.wait(10000); // 最多等待10秒
                }
                throw new RuntimeException(idempotent.message());
            }

            // 再次检查（双重检查）
            existingResult = checkAndGetResult(idempotentKey, idempotent);
            if (existingResult != null) {
                return existingResult;
            }

            // 执行原方法
            Object result = joinPoint.proceed();

            // 缓存结果
            saveResult(idempotentKey, result, idempotent);

            log.info("幂等性处理完成，缓存结果: {}", idempotentKey);
            return result;

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 生成幂等性key
     */
    private String generateIdempotentKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        // 使用 Signature 的 toShortString() 方法
        String signature = joinPoint.getSignature().toShortString();

        // 使用自定义前缀或默认值
        String prefix = idempotent.keyPrefix().isEmpty()
                ? signature
                : idempotent.keyPrefix();

        // 获取请求参数（简化处理）
        Object[] args = joinPoint.getArgs();
        String argsHash = args != null && args.length > 0
                ? String.valueOf(args[0].hashCode())
                : "empty";

        // 获取客户端信息
        String clientIp = IpUtil.getIpAddress(request);
        String sessionId = request.getSession().getId();

        // 构建完整的key
        String key = String.format("%s%s:%s:%s:%s",
                IDEMPOTENT_PREFIX,
                prefix,
                argsHash,
                clientIp,
                sessionId);

        return key;
    }

    /**
     * 检查并获取已缓存的结果
     */
    private Object checkAndGetResult(String key, Idempotent idempotent) {
        try {
            String lockKey = IDEMPOTENT_LOCK_PREFIX + key;
            RLock lock = redissonClient.getLock(lockKey);

            // 如果锁存在，说明正在处理
            if (lock.isLocked()) {
                return null;
            }

            // 检查是否有缓存的结果
            String resultKey = "result:" + key;
            String cached = (String) redissonClient.getBucket(resultKey).get();
            if (cached != null) {
                // 这里需要根据实际业务进行反序列化
                // 简化处理
                return null;
            }

            return null;
        } catch (Exception e) {
            log.error("检查幂等性结果失败: {}", key, e);
            return null;
        }
    }

    /**
     * 保存结果
     */
    private void saveResult(String key, Object result, Idempotent idempotent) {
        try {
            // 保存结果
            String resultKey = "result:" + key;
            redissonClient.getBucket(resultKey).set(resultKey, idempotent.expireTime(), TimeUnit.SECONDS);

            // 标记键存在（用于幂等性检查）
            String existsKey = "exists:" + key;
            redissonClient.getBucket(existsKey).set("1", idempotent.expireTime(), TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("保存幂等性结果失败: {}", key, e);
        }
    }
}
