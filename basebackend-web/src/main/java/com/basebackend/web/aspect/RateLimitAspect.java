package com.basebackend.web.aspect;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.basebackend.web.annotation.RateLimit;
import com.basebackend.web.util.IpUtil;
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

    private Object handleQpsLimit(ProceedingJoinPoint joinPoint, String resourceName, RateLimit rateLimit) throws Throwable {
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

    private Object handleThreadLimit(ProceedingJoinPoint joinPoint, String resourceName, RateLimit rateLimit) throws Throwable {
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

    private Object handleBlockException(String message) {
        // 返回限流或熔断的响应
        return new ApiResponse<>(500, message, null);
    }

    /**
     * API 响应封装
     */
    public static class ApiResponse<T> {
        private final int code;
        private final String message;
        private final T data;

        public ApiResponse(int code, String message, T data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public T getData() {
            return data;
        }
    }
}
