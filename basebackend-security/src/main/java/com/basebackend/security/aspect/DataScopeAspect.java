package com.basebackend.security.aspect;

import com.basebackend.security.annotation.DataScope;
import com.basebackend.security.context.DataScopeContextHolder;
import com.basebackend.security.enums.DataScopeType;
import com.basebackend.security.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * 数据权限切面
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE - 10) // 在权限切面之后执行
public class DataScopeAspect {

    private final PermissionService permissionService;

    @Around("@annotation(dataScope)")
    public Object around(ProceedingJoinPoint point, DataScope dataScope) throws Throwable {
        try {
            // 获取当前用户信息
            Long userId = permissionService.getCurrentUserId();
            Long deptId = permissionService.getCurrentUserDeptId();

            // 如果 deptId 为 null，则跳过数据权限检查（不进行过滤）
            if (deptId == null && dataScope.value() != DataScopeType.ALL) {
                log.warn("无法获取用户部门信息，跳过数据权限检查: userId={}", userId);
                return point.proceed();
            }

            // 设置完整的数据权限上下文（一次性设置，避免覆盖）
            DataScopeContextHolder.DataScopeContext context =
                new DataScopeContextHolder.DataScopeContext(dataScope.value(), userId, deptId);
            DataScopeContextHolder.set(context);

            log.debug("设置数据权限: userId={}, deptId={}, scope={}",
                    userId, deptId, dataScope.value());

            // 执行目标方法
            return point.proceed();

        } finally {
            // 清理数据权限上下文
            DataScopeContextHolder.clear();
            log.debug("清理数据权限上下文");
        }
    }
}
