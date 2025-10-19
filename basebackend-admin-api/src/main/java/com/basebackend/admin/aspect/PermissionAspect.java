package com.basebackend.admin.aspect;

import com.basebackend.admin.annotation.RequiresPermission;
import com.basebackend.admin.annotation.RequiresRole;
import com.basebackend.admin.annotation.DataScope;
import com.basebackend.admin.service.AuthService;
import com.basebackend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 权限校验切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final AuthService authService;

    /**
     * 权限校验切面
     */
    @Around("@annotation(requiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequiresPermission requiresPermission) throws Throwable {
        log.debug("权限校验: {}", requiresPermission.value());

        // 获取当前用户权限
        List<String> userPermissions = getCurrentUserPermissions();
        
        // 检查权限
        if (!hasPermission(userPermissions, requiresPermission)) {
            throw new BusinessException("权限不足");
        }

        return joinPoint.proceed();
    }

    /**
     * 角色校验切面
     */
    @Around("@annotation(requiresRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint, RequiresRole requiresRole) throws Throwable {
        log.debug("角色校验: {}", requiresRole.value());

        // 获取当前用户角色
        List<String> userRoles = getCurrentUserRoles();
        
        // 检查角色
        if (!hasRole(userRoles, requiresRole)) {
            throw new BusinessException("角色权限不足");
        }

        return joinPoint.proceed();
    }

    /**
     * 数据权限切面
     */
    @Around("@annotation(dataScope)")
    public Object checkDataScope(ProceedingJoinPoint joinPoint, DataScope dataScope) throws Throwable {
        log.debug("数据权限校验: {}", dataScope.value());

        // 这里可以添加数据权限的具体实现
        // 例如：根据用户的数据权限范围，动态修改SQL查询条件
        
        return joinPoint.proceed();
    }

    /**
     * 检查权限
     */
    private boolean hasPermission(List<String> userPermissions, RequiresPermission requiresPermission) {
        String[] permissions = requiresPermission.values().length > 0 ? 
            requiresPermission.values() : new String[]{requiresPermission.value()};
        
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
        String[] roles = requiresRole.values().length > 0 ? 
            requiresRole.values() : new String[]{requiresRole.value()};
        
        if (requiresRole.logical() == RequiresRole.Logical.AND) {
            // 需要所有角色
            return Arrays.stream(roles).allMatch(userRoles::contains);
        } else {
            // 需要任一角色
            return Arrays.stream(roles).anyMatch(userRoles::contains);
        }
    }

    /**
     * 获取当前用户权限
     */
    private List<String> getCurrentUserPermissions() {
        // 这里简化处理，实际应该从SecurityContext或ThreadLocal获取
        // 或者从Redis缓存中获取
        return List.of("system:user:list", "system:user:add", "system:user:edit");
    }

    /**
     * 获取当前用户角色
     */
    private List<String> getCurrentUserRoles() {
        // 这里简化处理，实际应该从SecurityContext或ThreadLocal获取
        // 或者从Redis缓存中获取
        return List.of("admin");
    }
}
