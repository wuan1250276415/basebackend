package com.basebackend.featuretoggle.annotation;

import java.lang.annotation.*;

/**
 * AB测试注解
 * 用于多变体实验
 *
 * @author BaseBackend
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ABTest {

    /**
     * 特性/实验名称
     */
    String value();

    /**
     * 变体名称（可选，不指定则自动获取）
     */
    String variantName() default "";

    /**
     * 是否记录实验数据
     */
    boolean track() default true;

    /**
     * 实验未启用时是否抛出异常
     */
    boolean throwException() default false;
}
