package com.basebackend.security.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 文件审计注解
 * 用于标记需要审计的文件操作
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FileAudit {

    /**
     * 操作类型 (UPLOAD, DOWNLOAD, DELETE, MOVE, COPY)
     */
    String operation() default "";

    /**
     * 文件类型
     */
    String fileType() default "";

    /**
     * 操作描述
     */
    String description() default "";
}
