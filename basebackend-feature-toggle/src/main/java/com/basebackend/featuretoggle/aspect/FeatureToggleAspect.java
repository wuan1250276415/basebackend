package com.basebackend.featuretoggle.aspect;

import com.basebackend.featuretoggle.abtest.RolloutCalculator;
import com.basebackend.featuretoggle.annotation.ABTest;
import com.basebackend.featuretoggle.annotation.FeatureToggle;
import com.basebackend.featuretoggle.annotation.GradualRollout;
import com.basebackend.featuretoggle.audit.FeatureToggleAuditService;
import com.basebackend.featuretoggle.context.FeatureContextHolder;
import com.basebackend.featuretoggle.exception.FeatureNotEnabledException;
import com.basebackend.featuretoggle.metrics.FeatureToggleMetrics;
import com.basebackend.featuretoggle.model.FeatureContext;
import com.basebackend.featuretoggle.model.Variant;
import com.basebackend.featuretoggle.service.FeatureToggleService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * 特性开关AOP切面
 * <p>
 * 支持三种注解：
 * <ul>
 *   <li>@FeatureToggle - 基础特性开关</li>
 *   <li>@GradualRollout - 灰度发布</li>
 *   <li>@ABTest - A/B测试</li>
 * </ul>
 * </p>
 *
 * @author BaseBackend
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(prefix = "feature-toggle", name = "enabled", havingValue = "true")
public class FeatureToggleAspect {

    private final FeatureToggleService featureToggleService;
    private final FeatureToggleMetrics featureToggleMetrics;
    private final FeatureToggleAuditService auditService;

    @Autowired
    public FeatureToggleAspect(FeatureToggleService featureToggleService,
                               @Autowired(required = false) FeatureToggleMetrics featureToggleMetrics,
                               @Autowired(required = false) FeatureToggleAuditService auditService) {
        this.featureToggleService = featureToggleService;
        this.featureToggleMetrics = featureToggleMetrics;
        this.auditService = auditService;
        log.info("FeatureToggleAspect initialized with provider: {}", featureToggleService.getProviderName());
    }

    /**
     * 处理 @FeatureToggle 注解
     */
    @Around("@annotation(com.basebackend.featuretoggle.annotation.FeatureToggle)")
    public Object aroundFeatureToggle(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        FeatureToggle annotation = method.getAnnotation(FeatureToggle.class);

        String featureName = annotation.value();
        boolean defaultValue = annotation.defaultValue();

        // 从持有器中获取上下文，支持HTTP与非HTTP场景
        FeatureContext context = FeatureContextHolder.get();

        boolean isEnabled = featureToggleService.isEnabled(featureName, context, defaultValue);
        long responseTime = System.currentTimeMillis() - startTime;

        // 记录指标
        recordMetrics(featureName, isEnabled, responseTime);

        // 记录审计日志
        recordAudit(featureName, "FEATURE_TOGGLE", isEnabled, context);

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

    /**
     * 处理 @GradualRollout 注解 - 灰度发布
     * <p>
     * 基于用户ID和百分比进行一致性分配，确保同一用户始终获得相同结果。
     * </p>
     */
    @Around("@annotation(com.basebackend.featuretoggle.annotation.GradualRollout)")
    public Object aroundGradualRollout(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        GradualRollout annotation = method.getAnnotation(GradualRollout.class);

        String featureName = annotation.value();
        int percentage = annotation.percentage();
        boolean stickySession = annotation.stickySession();
        GradualRollout.FallbackStrategy fallbackStrategy = annotation.fallbackStrategy();
        String fallbackMethodName = annotation.fallbackMethod();

        // 获取上下文
        FeatureContext context = FeatureContextHolder.get();

        // 构建灰度发布配置
        RolloutCalculator.RolloutConfig config = RolloutCalculator.RolloutConfig.newBuilder()
                .startPercentage(percentage)
                .endPercentage(percentage)
                .durationMinutes(1) // 固定比例模式
                .strategy(RolloutCalculator.RolloutStrategy.FIXED)
                .startTime(new Date(0)) // 立即生效
                .build();

        // 判断是否命中灰度
        boolean isInRollout;
        if (stickySession) {
            // 使用一致性哈希确保同一用户始终获得相同结果
            isInRollout = RolloutCalculator.shouldEnableFeature(featureName, context, config);
        } else {
            // 随机判断（不推荐，仅用于特殊场景）
            isInRollout = Math.random() * 100 < percentage;
        }

        long responseTime = System.currentTimeMillis() - startTime;

        // 记录指标
        recordMetrics(featureName, isInRollout, responseTime);

        // 记录审计日志
        recordAudit(featureName, "GRADUAL_ROLLOUT", isInRollout, context);

        log.debug("GradualRollout '{}': percentage={}, stickySession={}, inRollout={}, method={}",
                featureName, percentage, stickySession, isInRollout, method.getName());

        if (!isInRollout) {
            // 处理未命中灰度的情况
            return handleFallback(joinPoint, fallbackStrategy, fallbackMethodName, featureName);
        }

        return joinPoint.proceed();
    }

    /**
     * 处理 @ABTest 注解 - A/B测试
     * <p>
     * 获取用户分配的变体，并将变体信息注入到方法参数或上下文中。
     * </p>
     */
    @Around("@annotation(com.basebackend.featuretoggle.annotation.ABTest)")
    public Object aroundABTest(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        ABTest annotation = method.getAnnotation(ABTest.class);

        String featureName = annotation.value();
        String expectedVariantName = annotation.variantName();
        boolean track = annotation.track();
        boolean throwException = annotation.throwException();

        // 获取上下文
        FeatureContext context = FeatureContextHolder.get();

        // 获取变体信息
        Variant variant = featureToggleService.getVariant(featureName, context);

        long responseTime = System.currentTimeMillis() - startTime;

        // 检查变体是否启用
        boolean isEnabled = variant != null && Boolean.TRUE.equals(variant.getEnabled());

        // 如果指定了期望的变体名称，检查是否匹配
        if (isEnabled && !expectedVariantName.isEmpty() && variant != null) {
            isEnabled = expectedVariantName.equals(variant.getName());
        }

        // 记录指标
        recordMetrics(featureName, isEnabled, responseTime);

        // 记录A/B测试分组
        if (variant != null && featureToggleMetrics != null) {
            featureToggleMetrics.recordABTestAssignment(featureName, variant.getName());
        }

        // 记录审计日志
        String variantName = variant != null ? variant.getName() : "none";
        recordAuditWithVariant(featureName, "AB_TEST", isEnabled, context, variantName);

        // 如果需要追踪，记录实验数据
        if (track && variant != null) {
            log.info("ABTest tracking: feature={}, variant={}, userId={}, enabled={}",
                    featureName, variant.getName(),
                    context != null ? context.getUserId() : "anonymous",
                    isEnabled);
        }

        log.debug("ABTest '{}': variant={}, enabled={}, method={}",
                featureName, variantName, isEnabled, method.getName());

        if (!isEnabled) {
            if (throwException) {
                throw new FeatureNotEnabledException(featureName,
                        "A/B test '" + featureName + "' is not enabled for this user");
            }
            return null;
        }

        // 将变体信息存储到上下文中，供方法内部使用
        if (variant != null) {
            FeatureContextHolder.setCurrentVariant(variant);
        }

        try {
            return joinPoint.proceed();
        } finally {
            // 清理变体信息
            FeatureContextHolder.clearCurrentVariant();
        }
    }

    /**
     * 处理降级策略
     */
    private Object handleFallback(ProceedingJoinPoint joinPoint,
                                  GradualRollout.FallbackStrategy strategy,
                                  String fallbackMethodName,
                                  String featureName) throws Throwable {
        switch (strategy) {
            case RETURN_NULL:
                log.debug("GradualRollout '{}': returning null (fallback)", featureName);
                return null;

            case THROW_EXCEPTION:
                throw new FeatureNotEnabledException(featureName,
                        "Feature '" + featureName + "' is not enabled in gradual rollout");

            case FALLBACK_METHOD:
                if (fallbackMethodName == null || fallbackMethodName.isEmpty()) {
                    log.warn("GradualRollout '{}': fallback method not specified, returning null", featureName);
                    return null;
                }
                return invokeFallbackMethod(joinPoint, fallbackMethodName, featureName);

            default:
                return null;
        }
    }

    /**
     * 调用降级方法
     */
    private Object invokeFallbackMethod(ProceedingJoinPoint joinPoint,
                                        String fallbackMethodName,
                                        String featureName) {
        try {
            Object target = joinPoint.getTarget();
            Class<?> targetClass = target.getClass();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();

            // 查找降级方法（参数类型相同）
            Method fallbackMethod = targetClass.getMethod(fallbackMethodName, signature.getParameterTypes());
            fallbackMethod.setAccessible(true);

            log.debug("GradualRollout '{}': invoking fallback method '{}'", featureName, fallbackMethodName);
            return fallbackMethod.invoke(target, joinPoint.getArgs());

        } catch (NoSuchMethodException e) {
            log.error("GradualRollout '{}': fallback method '{}' not found", featureName, fallbackMethodName);
            return null;
        } catch (Exception e) {
            log.error("GradualRollout '{}': failed to invoke fallback method '{}': {}",
                    featureName, fallbackMethodName, e.getMessage());
            return null;
        }
    }

    /**
     * 记录指标
     */
    private void recordMetrics(String featureName, boolean enabled, long responseTime) {
        if (featureToggleMetrics != null) {
            featureToggleMetrics.recordCall(featureName, enabled, responseTime);
        }
    }

    /**
     * 记录审计日志
     */
    private void recordAudit(String featureName, String operationType, boolean result, FeatureContext context) {
        if (auditService != null) {
            auditService.recordAccess(featureName, operationType, result, context);
        }
    }

    /**
     * 记录带变体的审计日志
     */
    private void recordAuditWithVariant(String featureName, String operationType,
                                        boolean result, FeatureContext context, String variantName) {
        if (auditService != null) {
            auditService.recordAccessWithVariant(featureName, operationType, result, context, variantName);
        }
    }
}
