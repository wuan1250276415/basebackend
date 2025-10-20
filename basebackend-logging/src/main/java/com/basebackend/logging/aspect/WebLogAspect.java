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
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
        Object[] safeArgs = Arrays.stream(joinPoint.getArgs()).map(this::simplifyArg).toArray();
        try {
            log.info("Request Args   : {}", JSON.toJSONString(safeArgs));
        } catch (Throwable ignore) {
            log.info("Request Args   : {}", Arrays.toString(safeArgs));
        }
        // 获取请求信息
//        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//        if (attributes != null) {
//            HttpServletRequest request = attributes.getRequest();
//
//            // 记录请求信息
//            log.info("========================================== Request Start ==========================================");
//            log.info("URL            : {}", request.getRequestURL().toString());
//            log.info("HTTP Method    : {}", request.getMethod());
//            log.info("Class Method   : {}.{}", joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
//            log.info("IP             : {}", getIpAddress(request));
//            log.info("Request Args   : {}", JSON.toJSONString(joinPoint.getArgs()));
//        }

        // 执行方法
        Object result = joinPoint.proceed();

        // 记录响应信息
        long endTime = System.currentTimeMillis();
        log.info("Response Args  : {}", JSON.toJSONString(result));
        log.info("Time-Consuming : {} ms", endTime - startTime);
        log.info("========================================== Request End ==========================================");


        // 3) 记录响应：兜底
        try {
            log.info("Response Args  : {}", JSON.toJSONString(result));
        } catch (Throwable ignore) {
            log.info("Response Args  : {}", String.valueOf(result));
        }
        return result;
    }


    // 4) 新增方法：参数简化
    private Object simplifyArg(Object arg) {
        if (arg == null) return null;
        if (arg instanceof HttpServletRequest) return "HttpServletRequest";
        if (arg instanceof HttpServletResponse) return "HttpServletResponse";
        if (arg instanceof MultipartFile f) {
            Map<String, Object> info = new HashMap<>();
            info.put("type", "MultipartFile");
            info.put("name", f.getOriginalFilename());
            info.put("size", f.getSize());
            return info;
        }
        if (arg instanceof InputStream || arg instanceof Reader || arg instanceof byte[]) {
            return arg.getClass().getSimpleName();
        }
        return arg;
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
