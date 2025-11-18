package com.basebackend.security.aspect;

import com.basebackend.security.annotation.DataScope;
import com.basebackend.security.annotation.RequiresPermission;
import com.basebackend.security.annotation.RequiresRole;
import com.basebackend.security.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 权限校验切面
 * 
 * 自动校验标注了权限注解的方法
 * 
 * 使用方式：
 * 1. 在需要权限校验的方法上添加@RequiresPermission或@RequiresRole注解
 * 2. 实现PermissionService接口来提供权限信息
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnBean(PermissionService.class)
public class PermissionAspect {

    private final PermissionService permissionService;

    /**
     * 权限校验切面
     */
    @Around("@annotation(requiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequiresPermission requiresPermission) throws Throwable {
        log.debug("权限校验开始");

        // 获取当前用户权限
        List<String> userPermissions = permissionService.getCurrentUserPermissions();
        
        // 检查权限
        if (!hasPermission(userPermissions, requiresPermission)) {
            log.warn("权限不足，需要权限: {}, 用户权限: {}", 
                getRequiredPermissions(requiresPermission), userPermissions);
            throw new SecurityException("权限不足，无法访问该资源");
        }

        log.debug("权限校验通过");
        return joinPoint.proceed();
    }

    /**
     * 角色校验切面
     */
    @Around("@annotation(requiresRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint, RequiresRole requiresRole) throws Throwable {
        log.debug("角色校验开始");

        // 获取当前用户角色
        List<String> userRoles = permissionService.getCurrentUserRoles();
        
        // 检查角色
        if (!hasRole(userRoles, requiresRole)) {
            log.warn("角色权限不足，需要角色: {}, 用户角色: {}", 
                getRequiredRoles(requiresRole), userRoles);
            throw new SecurityException("角色权限不足，无法访问该资源");
        }

        log.debug("角色校验通过");
        return joinPoint.proceed();
    }

    /**
     * 数据权限切面
     */
    @Around("@annotation(dataScope)")
    public Object checkDataScope(ProceedingJoinPoint joinPoint, DataScope dataScope) throws Throwable {
        log.debug("数据权限校验: {}", dataScope.value());

        // 数据权限的具体实现由各服务自行处理
        // 这里只是一个切入点，可以在方法执行前后添加数据过滤逻辑
        
        // 可以通过ThreadLocal传递数据权限信息给MyBatis拦截器
        // 例如：DataScopeContextHolder.set(dataScope.value());
        
        try {
            return joinPoint.proceed();
        } finally {
            // 清理ThreadLocal
            // DataScopeContextHolder.clear();
        }
    }

    /**
     * 检查权限
     */
    private boolean hasPermission(List<String> userPermissions, RequiresPermission requiresPermission) {
        // 超级管理员拥有所有权限
        if (userPermissions.contains("*:*:*") || userPermissions.contains("*")) {
            return true;
        }

        String[] permissions = getRequiredPermissions(requiresPermission);
        
        if (requiresPermission.logical() == RequiresPermission.Logical.AND) {
            // 需要所有权限
            return Arrays.stream(permissions).allMatch(userPermissions::contains);
        } else {
            // 需要任一权限
            return Arrays.stream(permissions).anyMatch(userPermissions::contains);
        }
    }

    /**
     * 检查角色
     */
    private boolean hasRole(List<String> userRoles, RequiresRole requiresRole) {
        // 超级管理员拥有所有角色
        if (userRoles.contains("admin") || userRoles.contains("super_admin")) {
            return true;
        }

        String[] roles = getRequiredRoles(requiresRole);
        
        if (requiresRole.logical() == RequiresRole.Logical.AND) {
            // 需要所有角色
            return Arrays.stream(roles).allMatch(userRoles::contains);
        } else {
            // 需要任一角色
            return Arrays.stream(roles).anyMatch(userRoles::contains);
        }
    }

    /**
     * 获取需要的权限列表
     */
    private String[] getRequiredPermissions(RequiresPermission requiresPermission) {
        if (requiresPermission.values().length > 0) {
            return requiresPermission.values();
        }
        return new String[]{requiresPermission.value()};
    }

    /**
     * 获取需要的角色列表
     */
    private String[] getRequiredRoles(RequiresRole requiresRole) {
        if (requiresRole.values().length > 0) {
            return requiresRole.values();
        }
        return new String[]{requiresRole.value()};
    }
}
