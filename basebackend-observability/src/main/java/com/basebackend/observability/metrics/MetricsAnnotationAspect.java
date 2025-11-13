package com.basebackend.observability.metrics;

import com.basebackend.observability.metrics.annotations.Counted;
import com.basebackend.observability.metrics.annotations.Metered;
import com.basebackend.observability.metrics.annotations.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * 指标注解切面
 * 自动处理 @Timed、@Counted、@Metered 等指标注解
 */
@Slf4j
@Aspect
@Component
@Order(100)  // 确保在其他切面之后执行
@RequiredArgsConstructor
public class MetricsAnnotationAspect {

    private final MeterRegistry meterRegistry;

    /**
     * 处理 @Timed 注解
     */
    @Around("@annotation(com.basebackend.observability.metrics.annotations.Timed)")
    public Object handleTimed(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Timed timed = method.getAnnotation(Timed.class);

        // 获取指标名称
        String metricName = getMetricName(timed.name(), method);

        // 构建 Timer
        Timer.Builder timerBuilder = Timer.builder(metricName)
                .description(timed.description());

        // 添加自定义标签
        addTags(timerBuilder, timed.tags());

        // 添加方法信息标签
        timerBuilder.tag("class", method.getDeclaringClass().getSimpleName())
                    .tag("method", method.getName());

        // 配置百分位
        if (timed.percentiles()) {
            timerBuilder.publishPercentiles(0.5, 0.9, 0.95, 0.99);
        }

        // 配置直方图
        if (timed.histogram()) {
            timerBuilder.publishPercentileHistogram();
        }

        Timer timer = timerBuilder.register(meterRegistry);

        // 记录执行时间
        long startTime = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.nanoTime() - startTime;
            timer.record(Duration.ofNanos(duration));

            // 慢方法告警（超过 500ms）
            if (duration > 500_000_000L) {
                log.warn("Slow method detected: {}.{} - {}ms",
                        method.getDeclaringClass().getSimpleName(),
                        method.getName(),
                        duration / 1_000_000);
            }
        }
    }

    /**
     * 处理 @Counted 注解
     */
    @Around("@annotation(com.basebackend.observability.metrics.annotations.Counted)")
    public Object handleCounted(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Counted counted = method.getAnnotation(Counted.class);

        // 获取指标名称
        String metricName = getMetricName(counted.name(), method);

        Object result = null;
        Throwable exception = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            // 构建 Counter
            Counter.Builder counterBuilder = Counter.builder(metricName)
                    .description(counted.description());

            // 添加自定义标签
            addTags(counterBuilder, counted.tags());

            // 添加方法信息标签
            counterBuilder.tag("class", method.getDeclaringClass().getSimpleName())
                         .tag("method", method.getName());

            // 如果需要记录失败，添加 result 标签
            if (counted.recordFailures()) {
                String resultTag = exception != null ? "failure" : "success";
                counterBuilder.tag("result", resultTag);

                if (exception != null) {
                    counterBuilder.tag("exception", exception.getClass().getSimpleName());
                }
            }

            counterBuilder.register(meterRegistry).increment();
        }
    }

    /**
     * 处理 @Metered 注解
     */
    @Around("@annotation(com.basebackend.observability.metrics.annotations.Metered)")
    public Object handleMetered(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Metered metered = method.getAnnotation(Metered.class);

        // 获取指标名称
        String metricName = getMetricName(metered.name(), method);

        // Metered 使用 Counter 和 Timer 的组合
        // Counter 记录调用次数
        Counter.Builder counterBuilder = Counter.builder(metricName + ".calls")
                .description(metered.description() + " - call count");

        // Timer 记录平均响应时间
        Timer.Builder timerBuilder = Timer.builder(metricName + ".time")
                .description(metered.description() + " - response time")
                .publishPercentiles(0.5, 0.9, 0.99);

        // 添加自定义标签
        addTags(counterBuilder, metered.tags());
        addTags(timerBuilder, metered.tags());

        // 添加方法信息标签
        counterBuilder.tag("class", method.getDeclaringClass().getSimpleName())
                     .tag("method", method.getName());
        timerBuilder.tag("class", method.getDeclaringClass().getSimpleName())
                   .tag("method", method.getName());

        Counter counter = counterBuilder.register(meterRegistry);
        Timer timer = timerBuilder.register(meterRegistry);

        // 记录调用次数和响应时间
        long startTime = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            counter.increment();
            long duration = System.nanoTime() - startTime;
            timer.record(Duration.ofNanos(duration));
        }
    }

    /**
     * 获取指标名称
     * 如果注解中未指定名称，则使用方法全限定名
     */
    private String getMetricName(String annotationName, Method method) {
        if (annotationName != null && !annotationName.isEmpty()) {
            return annotationName;
        }
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    /**
     * 添加自定义标签到 Timer.Builder
     */
    private void addTags(Timer.Builder builder, String[] tags) {
        if (tags != null && tags.length > 0) {
            for (int i = 0; i < tags.length - 1; i += 2) {
                builder.tag(tags[i], tags[i + 1]);
            }
        }
    }

    /**
     * 添加自定义标签到 Counter.Builder
     */
    private void addTags(Counter.Builder builder, String[] tags) {
        if (tags != null && tags.length > 0) {
            for (int i = 0; i < tags.length - 1; i += 2) {
                builder.tag(tags[i], tags[i + 1]);
            }
        }
    }
}
