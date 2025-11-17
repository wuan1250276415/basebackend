package com.basebackend.security.audit;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 安全审计切面
 * 使用AOP自动记录敏感方法的执行
 */
@Slf4j
@Aspect
@Component
@Order(100)
public class SecurityAuditAspect {

    @Autowired
    private SecurityAuditService auditService;

    /**
     * 审计敏感操作
     * 使用@Audited注解标记的方法会自动记录
     */
    @Around("@annotation(com.basebackend.security.audit.Audited)")
    public Object auditSensitiveOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = getMethod(joinPoint);
        Audited audited = method.getAnnotation(Audited.class);

        String username = getCurrentUsername();
        String operation = audited.value();
        String resource = audited.resource();
        String action = method.getName();
        String ipAddress = getCurrentIpAddress();

        log.debug("开始执行敏感操作: 用户={}, 操作={}, 资源={}, 方法={}",
                username, operation, resource, action);

        long startTime = System.currentTimeMillis();
        boolean success = false;
        String errorMessage = null;

        try {
            Object result = joinPoint.proceed();
            success = true;
            log.debug("敏感操作执行成功: 用户={}, 操作={}, 资源={}",
                    username, operation, resource);
            return result;
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error("敏感操作执行失败: 用户={}, 操作={}, 错误={}",
                    username, operation, e.getMessage(), e);
            throw e;
        } finally {
            // 记录审计事件
            long duration = System.currentTimeMillis() - startTime;

            if (success) {
                auditService.logDataAccess(username, resource, action, ipAddress, true);
            } else {
                auditService.logDataAccess(username, resource, action, ipAddress, false);
            }
        }
    }

    /**
     * 审计数据修改操作
     * 自动记录创建、更新、删除操作
     */
    @Around("@annotation(com.basebackend.security.audit.DataAudit)")
    public Object auditDataModification(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = getMethod(joinPoint);
        DataAudit dataAudit = method.getAnnotation(DataAudit.class);

        String username = getCurrentUsername();
        String operation = dataAudit.operation();
        String tableName = dataAudit.table();
        String ipAddress = getCurrentIpAddress();

        log.debug("开始执行数据修改: 用户={}, 操作={}, 表={}",
                username, operation, tableName);

        long startTime = System.currentTimeMillis();
        boolean success = false;
        int recordCount = 0;

        try {
            Object result = joinPoint.proceed();
            success = true;

            // 获取影响的记录数
            if (result instanceof Integer) {
                recordCount = (Integer) result;
            } else if (result != null) {
                // 尝试从结果中推断记录数
                recordCount = 1;
            }

            log.debug("数据修改执行成功: 用户={}, 操作={}, 表={}, 记录数={}",
                    username, operation, tableName, recordCount);
            return result;
        } catch (Exception e) {
            log.error("数据修改执行失败: 用户={}, 操作={}, 表={}, 错误={}",
                    username, operation, tableName, e.getMessage(), e);
            throw e;
        } finally {
            // 记录审计事件
            auditService.logDatabaseAccess(username, tableName, operation, recordCount, ipAddress);
        }
    }

    /**
     * 审计权限操作
     */
    @Around("@annotation(com.basebackend.security.audit.PermissionAudit)")
    public Object auditPermissionOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = getMethod(joinPoint);
        PermissionAudit permissionAudit = method.getAnnotation(PermissionAudit.class);

        String username = getCurrentUsername();
        String action = permissionAudit.action();
        String target = permissionAudit.target();
        String ipAddress = getCurrentIpAddress();

        log.debug("执行权限操作: 用户={}, 操作={}, 目标={}",
                username, action, target);

        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            log.error("权限操作执行失败: 用户={}, 操作={}, 目标={}, 错误={}",
                    username, action, target, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 审计文件操作
     */
    @Around("@annotation(com.basebackend.security.audit.FileAudit)")
    public Object auditFileOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = getMethod(joinPoint);
        FileAudit fileAudit = method.getAnnotation(FileAudit.class);

        String username = getCurrentUsername();
        String operation = fileAudit.operation();
        String ipAddress = getCurrentIpAddress();

        log.debug("执行文件操作: 用户={}, 操作={}",
                username, operation);

        boolean success = false;
        String filePath = null;

        try {
            Object result = joinPoint.proceed();
            success = true;

            // 获取文件路径（从参数中提取）
            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] instanceof String) {
                filePath = (String) args[0];
            }

            log.debug("文件操作执行成功: 用户={}, 操作={}, 文件={}",
                    username, operation, filePath);
            return result;
        } catch (Exception e) {
            log.error("文件操作执行失败: 用户={}, 操作={}, 错误={}",
                    username, operation, e.getMessage(), e);
            throw e;
        } finally {
            // 记录审计事件
            if (filePath != null) {
                auditService.logFileOperation(username, filePath, operation, ipAddress, success);
            }
        }
    }

    /**
     * 获取当前用户名
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }

    /**
     * 获取当前IP地址
     */
    private String getCurrentIpAddress() {
        // 这里可以从ThreadLocal或请求上下文中获取
        // 实际实现中需要通过WebRequest获取
        return "0.0.0.0";
    }

    /**
     * 获取方法对象
     */
    private Method getMethod(ProceedingJoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        Class<?>[] parameterTypes = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature())
                .getParameterTypes();
        try {
            return joinPoint.getTarget().getClass().getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            log.error("获取方法失败", e);
            return null;
        }
    }
}
