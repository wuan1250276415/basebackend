package com.basebackend.logging.aspect;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Web请求日志切面
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
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // 记录请求信息
            log.info("========================================== Request Start ==========================================");
            log.info("URL            : {}", request.getRequestURL().toString());
            log.info("HTTP Method    : {}", request.getMethod());
            log.info("Class Method   : {}.{}", joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
            log.info("IP             : {}", getIpAddress(request));
            log.info("Request Args   : {}", JSON.toJSONString(joinPoint.getArgs()));
        }

        // 执行方法
        Object result = joinPoint.proceed();

        // 记录响应信息
        long endTime = System.currentTimeMillis();
        log.info("Response Args  : {}", JSON.toJSONString(result));
        log.info("Time-Consuming : {} ms", endTime - startTime);
        log.info("========================================== Request End ==========================================");

        return result;
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 对于多级代理，取第一个非unknown的IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0];
        }
        return ip;
    }
}
