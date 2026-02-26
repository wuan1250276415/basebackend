package com.basebackend.database.dynamic.aspect;

import com.basebackend.database.dynamic.annotation.DS;
import com.basebackend.database.dynamic.context.DataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 数据源切换切面
 * 处理 @DS 注解，实现动态数据源切换
 * 
 * @author basebackend
 */
@Slf4j
@Aspect
@Order(1) // 确保在事务切面之前执行
@Component
public class DataSourceAspect {
    
    /**
     * 定义切点：所有带 @DS 注解的方法
     */
    @Pointcut("@annotation(com.basebackend.database.dynamic.annotation.DS) " +
              "|| @within(com.basebackend.database.dynamic.annotation.DS)")
    public void dataSourcePointcut() {
    }
    
    /**
     * 环绕通知：切换数据源
     */
    @Around("dataSourcePointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        // 获取 @DS 注解
        DS ds = getDSAnnotation(point);
        
        if (ds == null) {
            // 如果没有注解，直接执行
            return point.proceed();
        }
        
        String dataSourceKey = ds.value();
        String previousDataSource = DataSourceContextHolder.getDataSourceKey();
        int stackDepthBefore = DataSourceContextHolder.getStackDepth();
        
        try {
            // 设置数据源
            DataSourceContextHolder.setDataSourceKey(dataSourceKey);
            int stackDepthAfter = DataSourceContextHolder.getStackDepth();
            
            // 增强的日志记录，显示嵌套层级和切换信息
            if (previousDataSource != null) {
                log.info("Nested datasource switch: [{}] -> [{}] (depth: {} -> {}) for method: {}", 
                    previousDataSource, dataSourceKey, stackDepthBefore, stackDepthAfter,
                    point.getSignature().toShortString());
            } else {
                log.info("Datasource switch: [{}] (depth: {}) for method: {}", 
                    dataSourceKey, stackDepthAfter, point.getSignature().toShortString());
            }
            
            // 执行目标方法
            return point.proceed();
            
        } finally {
            // 清除数据源（恢复到上一个数据源）
            DataSourceContextHolder.clearDataSourceKey();
            String restoredDataSource = DataSourceContextHolder.getDataSourceKey();
            int stackDepthAfterClear = DataSourceContextHolder.getStackDepth();
            
            // 记录恢复信息
            if (restoredDataSource != null) {
                log.info("Restored datasource: [{}] -> [{}] (depth: {}) after method: {}", 
                    dataSourceKey, restoredDataSource, stackDepthAfterClear,
                    point.getSignature().toShortString());
            } else {
                log.info("Cleared datasource: [{}] (depth: {}) after method: {}", 
                    dataSourceKey, stackDepthAfterClear, point.getSignature().toShortString());
            }
        }
    }
    
    /**
     * 获取 @DS 注解
     * 优先从方法上获取，如果方法上没有则从类上获取
     */
    private DS getDSAnnotation(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        
        // 先从方法上查找
        DS ds = AnnotationUtils.findAnnotation(method, DS.class);
        if (ds != null) {
            return ds;
        }
        
        // 再从类上查找
        Class<?> targetClass = point.getTarget().getClass();
        return AnnotationUtils.findAnnotation(targetClass, DS.class);
    }
}
