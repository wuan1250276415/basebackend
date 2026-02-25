package com.basebackend.database.observability;

import com.basebackend.observability.slo.annotation.SloMonitored;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Database 模块可观测性集成切面
 * <p>
 * 使用 Observation API 为数据库操作自动添加追踪和指标监控。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@ConditionalOnClass(name = "io.micrometer.observation.ObservationRegistry")
@ConditionalOnProperty(prefix = "basebackend.database.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseObservabilityAspect {

    private final ObservationRegistry observationRegistry;

    public DatabaseObservabilityAspect(
            @Autowired(required = false) ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry != null
                ? observationRegistry : ObservationRegistry.NOOP;
    }

    /**
     * 拦截 Mapper 方法，通过 Observation API 添加追踪和指标
     */
    @Around("execution(* com.basebackend..*.mapper..*.*(..))")
    @SloMonitored(sloName = "database-operations", service = "database-service")
    public Object traceDatabaseOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        Observation observation = Observation.createNotStarted(
                        "database.operation", observationRegistry)
                .lowCardinalityKeyValue("db.operation", determineOperation(methodName))
                .lowCardinalityKeyValue("db.mapper", className)
                .lowCardinalityKeyValue("db.method", methodName)
                .contextualName("db." + className + "." + methodName);

        return observation.observeChecked(() -> joinPoint.proceed());
    }

    private String determineOperation(String methodName) {
        if (methodName.startsWith("select") || methodName.startsWith("get") || methodName.startsWith("find") || methodName.startsWith("list")) {
            return "SELECT";
        } else if (methodName.startsWith("insert") || methodName.startsWith("save") || methodName.startsWith("add")) {
            return "INSERT";
        } else if (methodName.startsWith("update") || methodName.startsWith("modify")) {
            return "UPDATE";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "DELETE";
        } else {
            return "UNKNOWN";
        }
    }
}
