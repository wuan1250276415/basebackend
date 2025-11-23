package com.basebackend.security.aspect;

import com.basebackend.security.annotation.RequiresPermission;
import com.basebackend.security.annotation.RequiresRole;
import com.basebackend.security.service.PermissionService;
import com.basebackend.security.starter.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 安全审计切面
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Slf4j
@Aspect
@Component
@Order(200) // 在权限切面之后执行
@RequiredArgsConstructor
public class SecurityAuditAspect {

    private final PermissionService permissionService;
    private final SecurityProperties securityProperties;

    @Around("@annotation(requiresPermission)")
    public Object auditPermissionCheck(ProceedingJoinPoint point, RequiresPermission requiresPermission) throws Throwable {
        Long userId = permissionService.getCurrentUserId();
        String methodName = point.getSignature().toShortString();
        String[] permissions = new String[]{requiresPermission.value()};

        try {
            // 执行目标方法
            Object result = point.proceed();

            // 根据配置决定是否记录成功日志
            if (userId != null && securityProperties.getAudit().isLogSuccess()) {
                log.info("权限校验成功: userId={}, method={}, permissions={}",
                        userId, methodName, Arrays.toString(permissions));
            }

            return result;
        } catch (Exception e) {
            // 根据配置决定是否记录失败日志
            if (userId != null && securityProperties.getAudit().isLogFailure()) {
                log.warn("权限校验失败: userId={}, method={}, permissions={}, error={}",
                        userId, methodName, Arrays.toString(permissions), e.getMessage());
            }
            throw e;
        }
    }

    @Around("@annotation(requiresRole)")
    public Object auditRoleCheck(ProceedingJoinPoint point, RequiresRole requiresRole) throws Throwable {
        Long userId = permissionService.getCurrentUserId();
        String methodName = point.getSignature().toShortString();
        String[] roles = new String[]{requiresRole.value()};

        try {
            // 执行目标方法
            Object result = point.proceed();

            // 根据配置决定是否记录成功日志
            if (userId != null && securityProperties.getAudit().isLogSuccess()) {
                log.info("角色校验成功: userId={}, method={}, roles={}",
                        userId, methodName, Arrays.toString(roles));
            }

            return result;
        } catch (Exception e) {
            // 根据配置决定是否记录失败日志
            if (userId != null && securityProperties.getAudit().isLogFailure()) {
                log.warn("角色校验失败: userId={}, method={}, roles={}, error={}",
                        userId, methodName, Arrays.toString(roles), e.getMessage());
            }
            throw e;
        }
    }
}
