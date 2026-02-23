package com.basebackend.common.datascope.aspect;

import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.datascope.annotation.DataScope;
import com.basebackend.common.datascope.config.DataScopeProperties;
import com.basebackend.common.datascope.context.DataScopeContext;
import com.basebackend.common.datascope.handler.DataScopeSqlBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * 数据权限 AOP 切面
 * <p>
 * 处理 {@link DataScope} 注解，在方法执行前设置数据权限 SQL 条件，
 * 方法执行后清除。配合 {@link com.basebackend.common.datascope.interceptor.DataScopeInterceptor}
 * 在 SQL 执行时注入过滤条件。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class DataScopeAspect {

    private final DataScopeProperties properties;

    @Around("@annotation(dataScope)")
    public Object around(ProceedingJoinPoint joinPoint, DataScope dataScope) throws Throwable {
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        // 超管跳过数据权限
        if (properties.isSuperAdminSkip() && UserContextHolder.isSuperAdmin()) {
            log.debug("超管用户跳过数据权限过滤");
            return joinPoint.proceed();
        }

        try {
            String condition = DataScopeSqlBuilder.buildCondition(
                    dataScope.type(),
                    dataScope.deptAlias(),
                    dataScope.deptField(),
                    dataScope.userAlias(),
                    dataScope.userField(),
                    properties
            );

            if (condition != null && !condition.isEmpty()) {
                DataScopeContext.set(condition);
                log.debug("设置数据权限条件: {}", condition);
            }

            return joinPoint.proceed();
        } finally {
            DataScopeContext.clear();
        }
    }
}
