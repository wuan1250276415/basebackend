package com.basebackend.database.dynamic.annotation;

import java.lang.annotation.*;

/**
 * 动态数据源注解
 * 用于指定方法或类使用的数据源
 * 
 * @author basebackend
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DS {
    
    /**
     * 数据源名称
     * 默认使用主数据源
     */
    String value() default "master";
}
