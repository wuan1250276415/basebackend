package com.basebackend.featuretoggle.aspect;

import com.basebackend.featuretoggle.annotation.FeatureToggle;
import com.basebackend.featuretoggle.exception.FeatureNotEnabledException;
import com.basebackend.featuretoggle.model.FeatureContext;
import com.basebackend.featuretoggle.service.FeatureToggleService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 特性开关AOP切面
 *
 * @author BaseBackend
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(prefix = "feature-toggle", name = "enabled", havingValue = "true")
public class FeatureToggleAspect {

    private final FeatureToggleService featureToggleService;

    public FeatureToggleAspect(FeatureToggleService featureToggleService) {
        this.featureToggleService = featureToggleService;
        log.info("FeatureToggleAspect initialized with provider: {}", featureToggleService.getProviderName());
    }

    @Around("@annotation(com.basebackend.featuretoggle.annotation.FeatureToggle)")
    public Object aroundFeatureToggle(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        FeatureToggle annotation = method.getAnnotation(FeatureToggle.class);

        String featureName = annotation.value();
        boolean defaultValue = annotation.defaultValue();

        // 构建上下文（可从请求中获取用户信息，这里简化处理）
        FeatureContext context = FeatureContext.empty();

        boolean isEnabled = featureToggleService.isEnabled(featureName, context, defaultValue);

        if (!isEnabled) {
            log.debug("Feature '{}' is not enabled for method: {}", featureName, method.getName());

            if (annotation.throwException()) {
                throw new FeatureNotEnabledException(featureName, annotation.errorMessage());
            }

            // 返回null或默认值
            return null;
        }

        log.debug("Feature '{}' is enabled, proceeding with method execution", featureName);
        return joinPoint.proceed();
    }

    @Around("@annotation(com.basebackend.featuretoggle.annotation.GradualRollout)")
    public Object aroundGradualRollout(ProceedingJoinPoint joinPoint) throws Throwable {
        // 灰度发布逻辑
        log.debug("Gradual rollout check for method: {}", joinPoint.getSignature().getName());
        // 简化实现，实际应该根据百分比和用户ID进行判断
        return joinPoint.proceed();
    }

    @Around("@annotation(com.basebackend.featuretoggle.annotation.ABTest)")
    public Object aroundABTest(ProceedingJoinPoint joinPoint) throws Throwable {
        // AB测试逻辑
        log.debug("AB test check for method: {}", joinPoint.getSignature().getName());
        // 简化实现，实际应该获取变体信息并记录
        return joinPoint.proceed();
    }
}
