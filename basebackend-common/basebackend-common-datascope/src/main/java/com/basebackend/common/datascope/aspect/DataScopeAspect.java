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
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;

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

    @Around("execution(* *(..)) && (" +
            "@annotation(com.basebackend.common.datascope.annotation.DataScope) || " +
            "@within(com.basebackend.common.datascope.annotation.DataScope) || " +
            "@target(com.basebackend.common.datascope.annotation.DataScope))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        DataScope dataScope = resolveDataScope(joinPoint);
        if (dataScope == null) {
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

    private DataScope resolveDataScope(ProceedingJoinPoint joinPoint) {
        if (!(joinPoint.getSignature() instanceof MethodSignature methodSignature)) {
            return null;
        }

        Method method = methodSignature.getMethod();
        Object target = joinPoint.getTarget();
        Class<?> targetClass = target != null
                ? AopUtils.getTargetClass(target)
                : methodSignature.getDeclaringType();

        Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);

        DataScope methodScope = AnnotatedElementUtils.findMergedAnnotation(specificMethod, DataScope.class);
        if (methodScope != null) {
            return methodScope;
        }

        if (method != specificMethod) {
            methodScope = AnnotatedElementUtils.findMergedAnnotation(method, DataScope.class);
            if (methodScope != null) {
                return methodScope;
            }
        }

        return targetClass != null
                ? AnnotatedElementUtils.findMergedAnnotation(targetClass, DataScope.class)
                : null;
    }
}
