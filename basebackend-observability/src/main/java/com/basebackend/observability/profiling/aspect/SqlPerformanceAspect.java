package com.basebackend.observability.profiling.aspect;

import com.basebackend.observability.entity.SlowSqlRecord;
import com.basebackend.observability.mapper.SlowSqlRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * SQL性能监控切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SqlPerformanceAspect {

    private final SlowSqlRecordMapper slowSqlRecordMapper;

    @Value("${observability.profiling.sql.slow-threshold:1000}")
    private long slowSqlThreshold; // 默认1秒

    /**
     * 监控Mapper方法执行
     */
    @Around("execution(* com.basebackend..mapper.*.*(..))")
    public Object monitorSql(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        String traceId = MDC.get("traceId");
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            // 记录慢SQL
            if (duration > slowSqlThreshold) {
                recordSlowSql(methodName, duration, joinPoint.getArgs(), traceId);
                
                log.warn("Slow SQL detected: {} took {}ms", methodName, duration);
                
                // 超过5秒发送告警
                if (duration > 5000) {
                    sendSlowSqlAlert(methodName, duration);
                }
            }
            
            return result;
            
        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("SQL execution failed: {} took {}ms", methodName, duration, t);
            throw t;
        }
    }

    /**
     * 记录慢SQL
     */
    private void recordSlowSql(String methodName, long duration, Object[] args, String traceId) {
        try {
            SlowSqlRecord record = new SlowSqlRecord();
            record.setMethodName(methodName);
            record.setDuration(duration);
            record.setParameters(Arrays.toString(args));
            record.setTraceId(traceId);
            record.setTimestamp(LocalDateTime.now());
            
            // 尝试提取服务名
            String serviceName = System.getProperty("spring.application.name", "unknown");
            record.setServiceName(serviceName);
            
            slowSqlRecordMapper.insert(record);
            
        } catch (Exception e) {
            log.error("Failed to record slow SQL", e);
        }
    }

    /**
     * 发送慢SQL告警
     */
    private void sendSlowSqlAlert(String methodName, long duration) {
        // TODO: 集成告警服务
        log.warn("⚠️ ALERT: Very slow SQL - {} took {}ms", methodName, duration);
    }
}
