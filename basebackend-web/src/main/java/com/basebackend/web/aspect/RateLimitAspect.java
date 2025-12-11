package com.basebackend.web.aspect;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.basebackend.common.enums.CommonErrorCode;
import com.basebackend.common.util.IpUtil;
import com.basebackend.common.web.ResponseResult;
import com.basebackend.web.annotation.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Sentinel 限流切面
 * 使用 @RateLimit 注解实现限流控制
 *
 * @author basebackend
 * @since 2025-11-23
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final HttpServletRequest request;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 获取限流资源名称
        String resourceName = getResourceName(joinPoint, rateLimit);

        // 判断限流类型
        if (rateLimit.limitType() == RateLimit.LimitType.QPS) {
            return handleQpsLimit(joinPoint, resourceName, rateLimit);
        } else {
            return handleThreadLimit(joinPoint, resourceName, rateLimit);
        }
    }

    private Object handleQpsLimit(ProceedingJoinPoint joinPoint, String resourceName, RateLimit rateLimit)
            throws Throwable {
        Entry entry = null;
        try {
            // 进入 Sentinel 限流规则检查
            ContextUtil.enter(resourceName, getOrigin());

            entry = SphU.entry(resourceName, EntryType.IN, 1, rateLimit.threshold());

            // 执行原方法
            return joinPoint.proceed();

        } catch (BlockException ex) {
            log.warn("Rate limit triggered for resource: {} - {}", resourceName, ex.getMessage());
            return handleBlockException(rateLimit.message());
        } catch (Throwable ex) {
            // 记录业务异常（不属于限流熔断异常）
            Tracer.trace(ex);
            throw ex;
        } finally {
            if (entry != null) {
                entry.exit();
            }
            ContextUtil.exit();
        }
    }

    private Object handleThreadLimit(ProceedingJoinPoint joinPoint, String resourceName, RateLimit rateLimit)
            throws Throwable {
        Entry entry = null;
        try {
            ContextUtil.enter(resourceName, getOrigin());

            entry = SphU.entry(resourceName, EntryType.IN);

            return joinPoint.proceed();

        } catch (BlockException ex) {
            log.warn("Thread limit triggered for resource: {} - {}", resourceName, ex.getMessage());
            return handleBlockException(rateLimit.message());
        } catch (Throwable ex) {
            Tracer.trace(ex);
            throw ex;
        } finally {
            if (entry != null) {
                entry.exit();
            }
            ContextUtil.exit();
        }
    }

    private String getResourceName(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        // 如果注解中指定了资源名称，使用注解中的
        if (!rateLimit.resource().isEmpty()) {
            return rateLimit.resource();
        }

        // 默认使用类名.方法名
        return joinPoint.getSignature().toShortString();
    }

    private String getOrigin() {
        // 从请求头或参数中获取调用来源
        String origin = request.getHeader("X-Request-Source");
        if (origin == null) {
            origin = IpUtil.getIpAddress(request);
        }
        return origin;
    }

    /**
     * 处理限流阻断异常
     * 使用统一的 ResponseResult 响应格式和 CommonErrorCode 错误码
     *
     * @param message 限流提示消息
     * @return 统一格式的限流响应
     */
    private Object handleBlockException(String message) {
        // 使用统一的响应类和错误码
        return ResponseResult.error(CommonErrorCode.TOO_MANY_REQUESTS, message);
    }
}
