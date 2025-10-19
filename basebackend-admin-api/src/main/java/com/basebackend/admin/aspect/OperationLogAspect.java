package com.basebackend.admin.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.basebackend.admin.entity.SysOperationLog;
import com.basebackend.admin.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysOperationLogMapper operationLogMapper;

    /**
     * 操作日志切面
     */
    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        
        // 获取方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 构建操作日志
        SysOperationLog operationLog = new SysOperationLog();
        operationLog.setOperation(getOperationName(method));
        operationLog.setMethod(method.getName());
        operationLog.setParams(getRequestParams(joinPoint));
        operationLog.setIpAddress(getClientIpAddress(request));
        operationLog.setLocation(""); // 可以集成IP地址解析服务
        operationLog.setStatus(1); // 默认成功
        operationLog.setOperationTime(LocalDateTime.now());
        
        try {
            // 执行方法
            Object result = joinPoint.proceed();
            
            // 计算执行时间
            long endTime = System.currentTimeMillis();
            operationLog.setTime(endTime - startTime);
            
            // 保存操作日志
            operationLogMapper.insert(operationLog);
            
            return result;
        } catch (Exception e) {
            // 计算执行时间
            long endTime = System.currentTimeMillis();
            operationLog.setTime(endTime - startTime);
            operationLog.setStatus(0); // 失败
            operationLog.setErrorMsg(e.getMessage());
            
            // 保存操作日志
            operationLogMapper.insert(operationLog);
            
            throw e;
        }
    }

    /**
     * 获取操作名称
     */
    private String getOperationName(Method method) {
        String methodName = method.getName();
        
        if (methodName.startsWith("create") || methodName.startsWith("add")) {
            return "新增";
        } else if (methodName.startsWith("update") || methodName.startsWith("edit")) {
            return "修改";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "删除";
        } else if (methodName.startsWith("export")) {
            return "导出";
        } else if (methodName.startsWith("import")) {
            return "导入";
        } else {
            return "其他";
        }
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
            return JSONUtil.toJsonStr(args);
        } catch (Exception e) {
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
}
