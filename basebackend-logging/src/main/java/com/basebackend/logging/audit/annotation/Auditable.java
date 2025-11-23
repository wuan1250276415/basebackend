package com.basebackend.logging.audit.annotation;

import com.basebackend.logging.audit.AuditEventType;
import java.lang.annotation.*;

/**
 * 审计注解
 *
 * 用于标注需要审计的方法，自动记录操作日志。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /**
     * 审计事件类型
     */
    AuditEventType value();

    /**
     * 操作资源描述
     */
    String resource() default "";

    /**
     * 是否记录参数
     */
    boolean recordParams() default true;

    /**
     * 是否记录返回值
     */
    boolean recordResult() default false;

    /**
     * 是否记录异常
     */
    boolean recordException() default true;

    /**
     * 严重级别阈值（仅记录此级别及以上的事件）
     */
    int minSeverityLevel() default 1;
}
