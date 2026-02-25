package com.basebackend.observability.metrics;

import com.basebackend.observability.metrics.annotations.Counted;
import com.basebackend.observability.metrics.annotations.Metered;
import com.basebackend.observability.metrics.annotations.Timed;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 指标注解切面
 * <p>
 * 使用 Observation API 自动处理 @Timed、@Counted、@Metered 等指标注解，
 * 统一产生 Timer + Counter 指标和分布式追踪 Span。
 * </p>
 */
@Slf4j
@Aspect
@Component
@Order(100)
@RequiredArgsConstructor
public class MetricsAnnotationAspect {

    private final ObservationRegistry observationRegistry;

    /**
     * 处理 @Timed 注解
     */
    @Around("@annotation(com.basebackend.observability.metrics.annotations.Timed)")
    public Object handleTimed(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Timed timed = method.getAnnotation(Timed.class);

        String metricName = getMetricName(timed.name(), method);

        Observation observation = Observation.createNotStarted(metricName, observationRegistry)
                .lowCardinalityKeyValue("class", method.getDeclaringClass().getSimpleName())
                .lowCardinalityKeyValue("method", method.getName())
                .contextualName(metricName);

        addTagsToObservation(observation, timed.tags());

        return observation.observeChecked(() -> joinPoint.proceed());
    }

    /**
     * 处理 @Counted 注解
     */
    @Around("@annotation(com.basebackend.observability.metrics.annotations.Counted)")
    public Object handleCounted(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Counted counted = method.getAnnotation(Counted.class);

        String metricName = getMetricName(counted.name(), method);

        Observation observation = Observation.createNotStarted(metricName, observationRegistry)
                .lowCardinalityKeyValue("class", method.getDeclaringClass().getSimpleName())
                .lowCardinalityKeyValue("method", method.getName())
                .contextualName(metricName);

        addTagsToObservation(observation, counted.tags());

        return observation.observeChecked(() -> joinPoint.proceed());
    }

    /**
     * 处理 @Metered 注解
     */
    @Around("@annotation(com.basebackend.observability.metrics.annotations.Metered)")
    public Object handleMetered(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Metered metered = method.getAnnotation(Metered.class);

        String metricName = getMetricName(metered.name(), method);

        Observation observation = Observation.createNotStarted(metricName, observationRegistry)
                .lowCardinalityKeyValue("class", method.getDeclaringClass().getSimpleName())
                .lowCardinalityKeyValue("method", method.getName())
                .contextualName(metricName);

        addTagsToObservation(observation, metered.tags());

        return observation.observeChecked(() -> joinPoint.proceed());
    }

    private String getMetricName(String annotationName, Method method) {
        if (annotationName != null && !annotationName.isEmpty()) {
            return annotationName;
        }
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    private void addTagsToObservation(Observation observation, String[] tags) {
        if (tags != null && tags.length > 0) {
            for (int i = 0; i < tags.length - 1; i += 2) {
                observation.lowCardinalityKeyValue(tags[i], tags[i + 1]);
            }
        }
    }
}
