package com.basebackend.security.aspect;

import com.basebackend.security.annotation.RequiresPermission;
import com.basebackend.security.annotation.RequiresRole;
import com.basebackend.security.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
            log.warn("权限不足，需要权限: {}, 用户权限数量: {}",
                Arrays.toString(getRequiredPermissions(requiresPermission)),
                userPermissions == null ? 0 : userPermissions.size());
            throw new AccessDeniedException("权限不足，无法访问该资源");
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
            log.warn("角色权限不足，需要角色: {}, 用户角色数量: {}",
                Arrays.toString(getRequiredRoles(requiresRole)),
                userRoles == null ? 0 : userRoles.size());
            throw new AccessDeniedException("角色权限不足，无法访问该资源");
        }

        log.debug("角色校验通过");
        return joinPoint.proceed();
    }

    /**
     * 检查权限
     */
    private boolean hasPermission(List<String> userPermissions, RequiresPermission requiresPermission) {
        Set<String> required = Arrays.stream(getRequiredPermissions(requiresPermission))
            .filter(s -> s != null && !s.isBlank())
            .collect(Collectors.toSet());
        if (required.isEmpty()) {
            return false;
        }
        if (userPermissions == null || userPermissions.isEmpty()) {
            return false;
        }
        Set<String> userSet = new HashSet<>(userPermissions);
        if (userSet.contains("*") || userSet.contains("*:*")) {
            return true;
        }
        if (requiresPermission.logical() == RequiresPermission.Logical.AND) {
            return required.stream().allMatch(userSet::contains);
        }
        return required.stream().anyMatch(userSet::contains);
    }

    /**
     * 检查角色
     */
    private boolean hasRole(List<String> userRoles, RequiresRole requiresRole) {
        Set<String> required = Arrays.stream(getRequiredRoles(requiresRole))
            .filter(s -> s != null && !s.isBlank())
            .collect(Collectors.toSet());
        if (required.isEmpty()) {
            return false;
        }
        if (userRoles == null || userRoles.isEmpty()) {
            return false;
        }
        Set<String> userSet = new HashSet<>(userRoles);
        if (userSet.contains("super_admin") || userSet.contains("admin")) {
            return true;
        }
        if (requiresRole.logical() == RequiresRole.Logical.AND) {
            return required.stream().allMatch(userSet::contains);
        }
        return required.stream().anyMatch(userSet::contains);
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
