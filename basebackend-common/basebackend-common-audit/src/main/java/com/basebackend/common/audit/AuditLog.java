package com.basebackend.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    String module() default "";

    String action();

    String description() default "";

    boolean recordParams() default true;

    boolean recordResult() default false;
}
