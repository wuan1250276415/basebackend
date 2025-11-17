package com.basebackend.database.routing;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

/**
 * 数据源切换 AOP 切面
 * 根据注解和事务属性自动切换数据源
 *
 * 优先级顺序：
 * 1. @MasterOnly > @ReadOnly > @Transactional
 * 2. @Transactional 默认使用主库
 * 3. 无注解的查询方法使用从库
 *
 * @author 浮浮酱
 */
@Slf4j
@Aspect
@Component
@Order(1) // 确保在事务切面之前执行
public class DataSourceAspect {

    /**
     * 切入点：所有 Service 层方法
     */
    @Pointcut("execution(* com.basebackend..service..*.*(..))")
    public void serviceMethodPointcut() {
    }

    /**
     * 环绕通知：处理数据源切换
     */
    @Around("serviceMethodPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        // 1. 检查方法上的 @MasterOnly 注解（最高优先级）
        if (method.isAnnotationPresent(MasterOnly.class)) {
            DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);
            log.debug("检测到 @MasterOnly 注解，使用主库: {}", method.getName());
        }
        // 2. 检查方法上的 @ReadOnly 注解
        else if (method.isAnnotationPresent(ReadOnly.class)) {
            DataSourceContextHolder.setDataSourceType(DataSourceType.SLAVE);
            log.debug("检测到 @ReadOnly 注解，使用从库: {}", method.getName());
        }
        // 3. 检查方法上的 @Transactional 注解（事务默认使用主库）
        else if (method.isAnnotationPresent(Transactional.class)) {
            DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);
            log.debug("检测到 @Transactional 注解，使用主库: {}", method.getName());
        }
        // 4. 检查类上的注解
        else {
            Class<?> targetClass = point.getTarget().getClass();

            if (targetClass.isAnnotationPresent(MasterOnly.class)) {
                DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);
                log.debug("检测到类级别 @MasterOnly 注解，使用主库: {}", targetClass.getSimpleName());
            } else if (targetClass.isAnnotationPresent(ReadOnly.class)) {
                DataSourceContextHolder.setDataSourceType(DataSourceType.SLAVE);
                log.debug("检测到类级别 @ReadOnly 注解，使用从库: {}", targetClass.getSimpleName());
            } else {
                // 5. 默认规则：查询方法使用从库，其他使用主库
                String methodName = method.getName();
                if (isQueryMethod(methodName)) {
                    DataSourceContextHolder.setDataSourceType(DataSourceType.SLAVE);
                    log.debug("检测到查询方法，使用从库: {}", methodName);
                } else {
                    DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);
                    log.debug("检测到写操作方法，使用主库: {}", methodName);
                }
            }
        }

        try {
            return point.proceed();
        } finally {
            // 清除上下文，避免内存泄漏
            DataSourceContextHolder.clearDataSourceType();
        }
    }

    /**
     * 判断是否为查询方法
     * 根据方法名前缀判断
     *
     * @param methodName 方法名
     * @return 是否为查询方法
     */
    private boolean isQueryMethod(String methodName) {
        String lowerMethodName = methodName.toLowerCase();
        return lowerMethodName.startsWith("get")
                || lowerMethodName.startsWith("find")
                || lowerMethodName.startsWith("select")
                || lowerMethodName.startsWith("query")
                || lowerMethodName.startsWith("count")
                || lowerMethodName.startsWith("list")
                || lowerMethodName.startsWith("page")
                || lowerMethodName.startsWith("search")
                || lowerMethodName.startsWith("check")
                || lowerMethodName.startsWith("exist");
    }
}
