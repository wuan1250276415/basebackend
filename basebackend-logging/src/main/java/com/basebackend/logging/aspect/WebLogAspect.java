package com.basebackend.logging.aspect;

import com.alibaba.fastjson2.JSON;
import com.basebackend.logging.context.LogContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Web请求日志切面（结构化日志版本）
 * 自动记录API调用的详细信息，包括请求参数、响应结果、耗时等
 */
@Slf4j
@Aspect
@Component
public class WebLogAspect {

    /**
     * 切入点：所有Controller层的方法
     */
    @Pointcut("execution(public * com.basebackend..controller..*.*(..))")
    public void webLog() {
    }

    /**
     * 环绕通知：记录请求和响应信息
     */
    @Around("webLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        // 构建结构化日志信息
        Map<String, Object> logData = new HashMap<>();
        logData.put("traceId", LogContext.getTraceId());
        logData.put("requestId", LogContext.getRequestId());

        if (request != null) {
            logData.put("url", request.getRequestURL().toString());
            logData.put("method", request.getMethod());
            logData.put("ip", LogContext.getIpAddress());
            logData.put("uri", request.getRequestURI());
        }

        logData.put("classMethod", joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName());

        // 简化参数（避免敏感信息和大对象）
        Object[] safeArgs = Arrays.stream(joinPoint.getArgs())
                .map(this::simplifyArg)
                .toArray();
        logData.put("args", safeArgs);

        // 记录请求开始
        log.info("API Request - {}", JSON.toJSONString(logData));

        // 执行方法
        Object result = null;
        Throwable exception = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            // 计算耗时
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 构建响应日志
            Map<String, Object> responseLog = new HashMap<>();
            responseLog.put("traceId", LogContext.getTraceId());
            responseLog.put("requestId", LogContext.getRequestId());
            responseLog.put("duration", duration);
            responseLog.put("durationUnit", "ms");

            if (exception == null) {
                // 成功响应
                responseLog.put("status", "success");
                try {
                    responseLog.put("result", simplifyResult(result));
                } catch (Throwable ignore) {
                    responseLog.put("result", "Response too large or cannot be serialized");
                }
                log.info("API Response - {}", JSON.toJSONString(responseLog));
            } else {
                // 异常响应
                responseLog.put("status", "error");
                responseLog.put("errorType", exception.getClass().getSimpleName());
                responseLog.put("errorMessage", exception.getMessage());
                log.error("API Error - {}", JSON.toJSONString(responseLog), exception);
            }

            // 性能警告：响应时间超过1秒
            if (duration > 1000) {
                log.warn("Slow API detected - traceId: {}, duration: {}ms, method: {}",
                        LogContext.getTraceId(), duration, logData.get("classMethod"));
            }
        }
    }

    /**
     * 简化参数，避免敏感信息和大对象
     */
    private Object simplifyArg(Object arg) {
        if (arg == null) {
            return null;
        }

        if (arg instanceof HttpServletRequest) {
            return "[HttpServletRequest]";
        }

        if (arg instanceof HttpServletResponse) {
            return "[HttpServletResponse]";
        }

        if (arg instanceof MultipartFile file) {
            Map<String, Object> info = new HashMap<>();
            info.put("type", "MultipartFile");
            info.put("name", file.getOriginalFilename());
            info.put("size", file.getSize());
            info.put("contentType", file.getContentType());
            return info;
        }

        if (arg instanceof InputStream || arg instanceof Reader || arg instanceof byte[]) {
            return "[" + arg.getClass().getSimpleName() + "]";
        }

        // 检查是否包含敏感字段
        String argStr = String.valueOf(arg);
        if (argStr.toLowerCase().contains("password") ||
            argStr.toLowerCase().contains("token") ||
            argStr.toLowerCase().contains("secret")) {
            return "[Sensitive Data Hidden]";
        }

        return arg;
    }

    /**
     * 简化结果，避免超大响应
     */
    private Object simplifyResult(Object result) {
        if (result == null) {
            return null;
        }

        try {
            String resultStr = JSON.toJSONString(result);
            // 如果响应超过 10KB，截断
            if (resultStr.length() > 10240) {
                return "[Response too large: " + resultStr.length() + " characters]";
            }
            return result;
        } catch (Exception e) {
            return "[Cannot serialize response]";
        }
    }
}
