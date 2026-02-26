package com.basebackend.database.tenant.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import com.basebackend.database.tenant.context.TenantContext;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link IgnoreTenant} 注解切面
 * <p>
 * 在执行标注了 @IgnoreTenant 的方法时，临时清除租户上下文，
 * 方法执行完毕后恢复原租户上下文。
 */
@Slf4j
@Aspect
public class IgnoreTenantAspect {

    @Around("@annotation(ignoreTenant) || @within(ignoreTenant)")
    public Object around(ProceedingJoinPoint joinPoint, IgnoreTenant ignoreTenant) throws Throwable {
        String backup = TenantContext.getTenantId();
        try {
            TenantContext.clear();
            log.debug("@IgnoreTenant: 临时关闭租户过滤, method={}", joinPoint.getSignature().toShortString());
            return joinPoint.proceed();
        } finally {
            if (backup != null) {
                TenantContext.setTenantId(backup);
            }
            log.debug("@IgnoreTenant: 恢复租户上下文, tenantId={}", backup);
        }
    }
}
