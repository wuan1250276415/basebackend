package com.basebackend.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字符串安全校验注解，用于防止 XSS/SQL 注入等危险字符输入
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = SafeStringValidator.class)
public @interface SafeString {

    String message() default "输入内容包含非法字符";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 是否必填，默认非必填
     */
    boolean required() default false;

    /**
     * 允许的最大长度
     */
    int maxLength() default 512;
}
