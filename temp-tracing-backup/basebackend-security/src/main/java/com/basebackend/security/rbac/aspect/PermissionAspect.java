package com.basebackend.security.rbac.aspect;

import com.basebackend.security.rbac.EnhancedPermissionService;
import com.basebackend.security.rbac.PermissionContext;
import com.basebackend.security.rbac.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限切面
 * 自动处理权限验证、数据范围检查、角色验证等
 */
@Slf4j
@Aspect
@Component
@Order(200)
@RequiredArgsConstructor
public class PermissionAspect {

    private final EnhancedPermissionService permissionService;

    /**
     * 处理权限要求注解
     */
    @Around("@annotation(requirePermission)")
    public Object aroundRequirePermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 1. 获取当前用户ID
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new SecurityException("用户未登录");
        }

        // 2. 提取权限验证上下文
        PermissionContext context = extractPermissionContext(joinPoint, method, userId);

        // 3. 检查权限
        boolean hasPermission = checkPermission(requirePermission, userId, context);
        if (!hasPermission) {
            String message = StringUtils.hasText(requirePermission.message())
                    ? requirePermission.message()
                    : "权限不足: " + requirePermission.value();

            throw createException(requirePermission.exception(), message);
        }

        // 4. 检查数据范围
        if (requirePermission.checkDataScope()) {
            checkDataScope(requirePermission, userId, context);
        }

        // 5. 执行目标方法
        return joinPoint.proceed();
    }

    /**
     * 处理角色要求注解
     */
    @Around("@annotation(requireRole)")
    public Object aroundRequireRole(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 1. 获取当前用户ID
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new SecurityException("用户未登录");
        }

        // 2. 检查角色
        boolean hasRole = checkRole(requireRole, userId);
        if (!hasRole) {
            throw new SecurityException("角色不足: " + requireRole.value());
        }

        // 3. 检查角色是否活跃
        if (requireRole.activeOnly()) {
            checkRoleActive(requireRole, userId);
        }

        // 4. 执行目标方法
        return joinPoint.proceed();
    }

    /**
     * 处理数据范围检查注解
     */
    @Around("@annotation(dataScope)")
    public Object aroundDataScope(ProceedingJoinPoint joinPoint, DataScope dataScope) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 1. 获取当前用户ID
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new SecurityException("用户未登录");
        }

        // 2. 提取权限验证上下文
        PermissionContext context = extractPermissionContext(joinPoint, method, userId);

        // 3. 检查数据范围
        checkDataScope(dataScope, userId, context);

        // 4. 执行目标方法
        return joinPoint.proceed();
    }

    /**
     * 处理Owner验证注解
     */
    @Around("@annotation(requireOwner)")
    public Object aroundRequireOwner(ProceedingJoinPoint joinPoint, RequireOwner requireOwner) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 1. 获取当前用户ID
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new SecurityException("用户未登录");
        }

        // 2. 获取资源Owner ID
        Object[] args = joinPoint.getArgs();
        Long ownerId = extractOwnerId(args, method, requireOwner.ownerIdParam());

        if (ownerId != null && !ownerId.equals(userId)) {
            // 3. 检查是否有允许的角色
            boolean hasAllowedRole = checkAllowedRoles(requireOwner.allowedRoles(), userId);
            if (!hasAllowedRole) {
                throw new SecurityException("只有资源Owner或具有相应角色的用户才能访问此资源");
            }
        }

        // 4. 执行目标方法
        return joinPoint.proceed();
    }

    /**
     * 检查权限
     */
    private boolean checkPermission(RequirePermission annotation, Long userId, PermissionContext context) {
        String[] permissions = annotation.value().split(",");
        Set<String> permissionSet = Arrays.stream(permissions)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        if (permissionSet.isEmpty()) {
            return true;
        }

        if (annotation.logic() == RequirePermission.Logic.ALL) {
            // 需要所有权限
            return permissionService.hasAllPermissions(userId, permissions);
        } else {
            // 任意一个权限即可
            return permissionService.hasAnyPermission(userId, permissions);
        }
    }

    /**
     * 检查角色
     */
    private boolean checkRole(RequireRole annotation, Long userId) {
        String[] roles = annotation.value().split(",");
        Set<String> roleSet = Arrays.stream(roles)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        if (roleSet.isEmpty()) {
            return true;
        }

        // TODO: 实现角色检查逻辑
        // 需要从数据库查询用户角色并进行比较
        return true;
    }

    /**
     * 检查角色是否活跃
     */
    private void checkRoleActive(RequireRole annotation, Long userId) {
        // TODO: 实现检查角色是否活跃的逻辑
        // 需要检查用户角色的有效期和状态
    }

    /**
     * 检查数据范围
     */
    private void checkDataScope(RequirePermission annotation, Long userId, PermissionContext context) {
        RequirePermission.DataScopeType scopeType = annotation.dataScope();

        if (scopeType == RequirePermission.DataScopeType.AUTO) {
            // 自动判断数据范围
            scopeType = getAutoDataScope(userId, context);
        }

        checkDataScope(scopeType, userId, context);
    }

    /**
     * 检查数据范围
     */
    private void checkDataScope(DataScope annotation, Long userId, PermissionContext context) {
        RequirePermission.DataScopeType scopeType = annotation.type();

        if (scopeType == RequirePermission.DataScopeType.AUTO) {
            // 自动判断数据范围
            scopeType = getAutoDataScope(userId, context);
        }

        checkDataScope(scopeType, userId, context);
    }

    /**
     * 检查数据范围
     */
    private void checkDataScope(RequirePermission.DataScopeType scopeType, Long userId, PermissionContext context) {
        switch (scopeType) {
            case ALL:
                // 全部数据，无需检查
                break;
            case SELF:
                // 仅本人数据
                if (context.getResourceOwnerId() != null &&
                        !context.getResourceOwnerId().equals(userId)) {
                    throw new SecurityException("只能访问自己的数据");
                }
                break;
            case DEPT:
                // 本部门数据
                if (!permissionService.checkDataScope(userId, context.getResourceOwnerId(), context.getDeptId())) {
                    throw new SecurityException("只能访问本部门的数据");
                }
                break;
            case DEPT_AND_CHILD:
                // 本部门及以下数据
                if (!permissionService.checkDataScope(userId, context.getResourceOwnerId(), context.getDeptId())) {
                    throw new SecurityException("只能访问本部门及以下的数据");
                }
                break;
            default:
                break;
        }
    }

    /**
     * 检查允许的角色
     */
    private boolean checkAllowedRoles(String[] allowedRoles, Long userId) {
        if (allowedRoles == null || allowedRoles.length == 0) {
            return false;
        }

        // TODO: 实现角色检查逻辑
        return true;
    }

    /**
     * 获取自动数据范围
     */
    private RequirePermission.DataScopeType getAutoDataScope(Long userId, PermissionContext context) {
        PermissionContext.DataScope dataScope = permissionService.getUserDataScope(userId);

        switch (dataScope) {
            case ALL:
                return RequirePermission.DataScopeType.ALL;
            case DEPT:
            case DEPT_AND_SUB:
            case DEPT_AND_CHILD:
                return RequirePermission.DataScopeType.DEPT_AND_CHILD;
            case SELF:
                return RequirePermission.DataScopeType.SELF;
            case CUSTOM:
                return RequirePermission.DataScopeType.CUSTOM;
            default:
                return RequirePermission.DataScopeType.SELF;
        }
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                // 从UserDetails中获取用户ID
                // TODO: 需要根据实际的用户详情结构实现
                return 1L; // 默认值
            }
        }
        return null;
    }

    /**
     * 提取权限验证上下文
     */
    private PermissionContext extractPermissionContext(ProceedingJoinPoint joinPoint, Method method, Long userId) {
        PermissionContext context = new PermissionContext();
        context.setUserId(userId);

        // 解析方法参数上的注解
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameterAnnotations.length; i++) {
            Annotation[] paramAnnotations = parameterAnnotations[i];

            for (Annotation annotation : paramAnnotations) {
                if (annotation instanceof PermissionContextParam) {
                    PermissionContextParam contextParam = (PermissionContextParam) annotation;
                    extractContextFromParameter(context, args[i], contextParam);
                }
            }
        }

        return context;
    }

    /**
     * 从参数中提取上下文信息
     */
    private void extractContextFromParameter(PermissionContext context, Object arg, PermissionContextParam annotation) {
        if (arg == null) {
            return;
        }

        // 使用反射从参数对象中提取属性
        try {
            java.lang.reflect.Field userIdField = arg.getClass().getDeclaredField(annotation.userId());
            userIdField.setAccessible(true);
            Object userId = userIdField.get(arg);
            if (userId instanceof Long) {
                context.setUserId((Long) userId);
            }

            if (StringUtils.hasText(annotation.resourceOwnerId())) {
                java.lang.reflect.Field ownerIdField = arg.getClass().getDeclaredField(annotation.resourceOwnerId());
                ownerIdField.setAccessible(true);
                Object ownerId = ownerIdField.get(arg);
                if (ownerId instanceof Long) {
                    context.setResourceOwnerId((Long) ownerId);
                }
            }

            if (StringUtils.hasText(annotation.deptId())) {
                java.lang.reflect.Field deptIdField = arg.getClass().getDeclaredField(annotation.deptId());
                deptIdField.setAccessible(true);
                Object deptId = deptIdField.get(arg);
                if (deptId instanceof Long) {
                    context.setDeptId((Long) deptId);
                }
            }

        } catch (Exception e) {
            log.debug("提取权限上下文失败", e);
        }
    }

    /**
     * 提取Owner ID
     */
    private Long extractOwnerId(Object[] args, Method method, String ownerIdParam) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        for (int i = 0; i < parameterAnnotations.length; i++) {
            Annotation[] paramAnnotations = parameterAnnotations[i];

            for (Annotation annotation : paramAnnotations) {
                if (annotation instanceof RequireOwner) {
                    return extractFieldValue(args[i], ownerIdParam, Long.class);
                }
            }
        }

        return null;
    }

    /**
     * 从对象中提取字段值
     */
    private <T> T extractFieldValue(Object obj, String fieldName, Class<T> fieldType) {
        if (obj == null || !StringUtils.hasText(fieldName)) {
            return null;
        }

        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(obj);
            return fieldType.cast(value);
        } catch (Exception e) {
            log.debug("提取字段值失败: {}", fieldName, e);
            return null;
        }
    }

    /**
     * 创建异常
     */
    private Throwable createException(Class<? extends Throwable> exceptionType, String message) {
        try {
            return exceptionType.getDeclaredConstructor(String.class).newInstance(message);
        } catch (Exception e) {
            return new SecurityException(message);
        }
    }
}
