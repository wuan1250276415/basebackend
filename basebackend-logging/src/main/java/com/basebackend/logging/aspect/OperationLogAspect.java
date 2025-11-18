package com.basebackend.logging.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.basebackend.logging.annotation.OperationLog;
import com.basebackend.logging.model.OperationLogInfo;
import com.basebackend.logging.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志切面
 * 
 * 自动记录标注了@OperationLog注解的方法的操作日志
 * 
 * 使用方式：
 * 1. 在需要记录日志的方法上添加@OperationLog注解
 * 2. 实现OperationLogService接口来保存日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnBean(OperationLogService.class)
public class OperationLogAspect {

    private final OperationLogService operationLogService;

    /**
     * 操作日志切面
     */
    @Around("@annotation(operationLog)")
    public Object logOperation(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 获取请求信息
        HttpServletRequest request = getHttpServletRequest();
        
        // 获取方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 构建操作日志
        OperationLogInfo logInfo = new OperationLogInfo();
        logInfo.setOperation(getOperationName(operationLog, method));
        logInfo.setBusinessType(operationLog.businessType());
        logInfo.setMethod(method.getDeclaringClass().getName() + "." + method.getName());
        
        if (operationLog.saveRequestData()) {
            logInfo.setParams(getRequestParams(joinPoint));
        }
        
        if (request != null) {
            logInfo.setIpAddress(getClientIpAddress(request));
        }
        
        logInfo.setStatus(1); // 默认成功
        logInfo.setOperationTime(LocalDateTime.now());
        
        try {
            // 执行方法
            Object result = joinPoint.proceed();
            
            // 计算执行时间
            long endTime = System.currentTimeMillis();
            logInfo.setTime(endTime - startTime);
            
            // 保存响应结果
            if (operationLog.saveResponseData() && result != null) {
                try {
                    logInfo.setResult(JSONUtil.toJsonStr(result));
                } catch (Exception e) {
                    logInfo.setResult("结果序列化失败");
                }
            }
            
            // 异步保存操作日志
            saveLogAsync(logInfo);
            
            return result;
        } catch (Exception e) {
            // 计算执行时间
            long endTime = System.currentTimeMillis();
            logInfo.setTime(endTime - startTime);
            logInfo.setStatus(0); // 失败
            logInfo.setErrorMsg(e.getMessage());
            
            // 异步保存操作日志
            saveLogAsync(logInfo);
            
            throw e;
        }
    }

    /**
     * 获取操作名称
     */
    private String getOperationName(OperationLog operationLog, Method method) {
        if (StrUtil.isNotBlank(operationLog.operation())) {
            return operationLog.operation();
        }
        
        // 根据业务类型返回默认名称
        return switch (operationLog.businessType()) {
            case INSERT -> "新增";
            case UPDATE -> "修改";
            case DELETE -> "删除";
            case SELECT -> "查询";
            case EXPORT -> "导出";
            case IMPORT -> "导入";
            case GRANT -> "授权";
            case FORCE_LOGOUT -> "强退";
            case CLEAN -> "清空";
            default -> "操作";
        };
    }

    /**
     * 获取请求参数
     */
    private String getRequestParams(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return "";
        }
        
        try {
            // 过滤掉HttpServletRequest、HttpServletResponse等对象
            Object[] filteredArgs = java.util.Arrays.stream(args)
                .filter(arg -> !(arg instanceof HttpServletRequest) 
                    && !(arg instanceof jakarta.servlet.http.HttpServletResponse))
                .toArray();
            
            if (filteredArgs.length == 0) {
                return "";
            }
            
            return JSONUtil.toJsonStr(filteredArgs);
        } catch (Exception e) {
            log.warn("参数序列化失败: {}", e.getMessage());
            return "参数解析失败";
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(xForwardedFor) && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StrUtil.isNotBlank(xRealIp) && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 获取HttpServletRequest
     */
    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 异步保存日志
     */
    private void saveLogAsync(OperationLogInfo logInfo) {
        try {
            operationLogService.saveOperationLog(logInfo);
        } catch (Exception e) {
            log.error("保存操作日志失败: {}", e.getMessage(), e);
        }
    }
}
