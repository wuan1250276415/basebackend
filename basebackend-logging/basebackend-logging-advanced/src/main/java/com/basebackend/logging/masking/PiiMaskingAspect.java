package com.basebackend.logging.masking;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;

/**
 * PII脱敏AOP切面
 *
 * 拦截标注了@DataMasking注解的方法，自动对返回值进行脱敏处理。
 *
 * <p>注意：此类不使用 {@code @Component}，由 {@link MaskingAutoConfiguration} 统一注册为 Spring Bean，
 * 避免与自动配置的 Bean 产生双重注册，导致每个 {@code @DataMasking} 方法被拦截两次。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Aspect
@Order(5)
public class PiiMaskingAspect {

    /**
     * 脱敏服务
     */
    private final PiiMaskingService service;

    /**
     * 配置属性
     */
    private final MaskingProperties properties;

    /**
     * 构造函数
     */
    public PiiMaskingAspect(PiiMaskingService service, MaskingProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    /**
     * 环绕通知：处理脱敏
     */
    @Around("@annotation(masking)")
    public Object around(ProceedingJoinPoint pjp, DataMasking masking) throws Throwable {
        // 检查是否启用脱敏
        if (!masking.enabled() || !properties.isEnabled()) {
            return pjp.proceed();
        }

        // 执行原方法
        Object result = pjp.proceed();

        // 处理null值
        if (result == null && masking.skipNull()) {
            return null;
        }

        // 应用脱敏
        return service.mask(result);
    }
}
