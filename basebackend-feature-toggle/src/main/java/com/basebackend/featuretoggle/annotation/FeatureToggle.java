package com.basebackend.featuretoggle.annotation;

import java.lang.annotation.*;

/**
 * 特性开关注解
 * 用于方法级别的特性开关控制
 *
 * @author BaseBackend
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FeatureToggle {

    /**
     * 特性名称
     */
    String value();

    /**
     * 特性未启用时是否抛出异常
     */
    boolean throwException() default false;

    /**
     * 特性未启用时的错误消息
     */
    String errorMessage() default "Feature is not enabled";

    /**
     * 默认值（当无法连接到特性开关服务时）
     */
    boolean defaultValue() default false;
}
